package com.saas.search.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.common.exception.BusinessException;
import com.saas.search.domain.document.BaseDocument;
import com.saas.search.domain.document.EmployeeBalanceDocument;
import com.saas.search.domain.document.LocationDocument;
import com.saas.search.domain.document.ThirdPartyDocument;
import com.saas.search.domain.document.UserDocument;
import com.saas.search.domain.constants.Entities;
import com.saas.search.infrastructure.client.AuthInternalClient;
import com.saas.search.infrastructure.client.FinanceInternalClient;
import com.saas.search.infrastructure.client.SystemInternalClient;
import com.saas.search.infrastructure.client.ThirdpartyInternalClient;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Reindex desde las fuentes (auth, system, thirdparty, finance) hacia Elasticsearch.
 *
 * <p>Tres granularidades, todas sobre el mismo pipeline:
 * <ul>
 *   <li>{@link #reindexOne(String, String)} — un registro concreto.</li>
 *   <li>{@link #reindexEntity(String)} — todos los registros de una entidad.</li>
 *   <li>{@link #reindexAll()} — todo lo que vive en Elastic.</li>
 * </ul>
 *
 * <p>Al arrancar se dispara {@code reindexAll()} si
 * {@code saas.search.reindex.enabled=true}; {@code saas.search.reindex.entities}
 * lo acota ({@code all} o lista separada por comas).
 *
 * <p>Cada entidad se declara UNA vez en {@link #sources()}: alias destino, de
 * que servicio viene, como se cuenta, como se pagina y como se pide un registro
 * suelto. Añadir una entidad nueva a Elastic es añadir una entrada ahi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReindexService {

    private static final int PAGE_SIZE = 500;
    /** Cada cuantos documentos se emite un aviso de progreso dentro de una pagina. */
    private static final int PROGRESS_EVERY = 25;

    /** Un solo hilo: los reindex largos corren fuera de la peticion HTTP y en fila. */
    private final ExecutorService reindexPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "reindex");
        t.setDaemon(true);
        return t;
    });

    /** Tiempo maximo a esperar a que Eureka tenga las instancias registradas. */
    private static final long EUREKA_WAIT_TIMEOUT_MS = 60_000;
    private static final long EUREKA_WAIT_POLL_MS    = 2_000;

    private final AuthInternalClient authClient;
    private final SystemInternalClient systemClient;
    private final ThirdpartyInternalClient thirdpartyClient;
    private final FinanceInternalClient financeClient;
    private final ElasticsearchOperations ops;
    private final ObjectMapper mapper;
    private final IndexNames indexNames;
    private final DiscoveryClient discoveryClient;
    private final ReindexProgress progress;

    @Value("${saas.search.reindex.enabled:false}")
    private boolean reindexEnabled;

    @Value("${saas.search.reindex.entities:all}")
    private String entitiesProperty;

    // ==================== CONTRATO PUBLICO ====================

    /** Resultado de un reindex: cuantos entraron y cuantos se cayeron. */
    public record ReindexResult(String entity, long indexed, long failed) {}

    /** Estado de un indice: cuanto hay en la fuente vs cuanto hay en Elastic. */
    public record IndexStatus(String entity, String index, Long source, long indexed, boolean sourceUp) {}

    /** Entidades que Elastic sabe reindexar. */
    public Set<String> supportedEntities() {
        return sources().keySet();
    }

    /** Reindexa TODO lo que vive en Elastic. */
    public List<ReindexResult> reindexAll() {
        return sources().keySet().stream().map(this::reindexEntity).toList();
    }

    /**
     * Lanza el reindex en segundo plano. Un reindex de entidad o completo tarda
     * minutos (una llamada por pagina a la fuente, mas el bulk a Elastic) y la
     * peticion HTTP muere esperando: el gateway corta y la pantalla ve un error
     * aunque el trabajo termine bien. Un solo hilo, ademas, serializa los
     * reindex: dos completos a la vez solo se estorbarian.
     *
     * @param entity entidad a reindexar, o {@code null} para todo.
     */
    public void reindexInBackground(String entity) {
        if (entity != null) require(entity); // valida ya, para poder responder 400
        String scope = entity == null ? "todo" : entity;
        reindexPool.submit(() -> {
            long startMillis = System.currentTimeMillis();
            progress.reset();
            progress.start(scope);
            try {
                List<ReindexResult> results = entity == null
                        ? reindexAll()
                        : List.of(reindexEntity(entity));
                progress.end(scope,
                        results.stream().mapToLong(ReindexResult::indexed).sum(),
                        results.stream().mapToLong(ReindexResult::failed).sum(),
                        System.currentTimeMillis() - startMillis);
            } catch (Exception ex) {
                log.error("Reindex en segundo plano de '{}' fallo: {}", scope, ex.getMessage(), ex);
                progress.failed(scope, ex.getMessage());
            }
        });
    }

    /** Reindexa todos los registros de una entidad. */
    public ReindexResult reindexEntity(String entity) {
        EntitySource source = require(entity);
        long indexed = 0;
        long failed = 0;
        for (PagedSource paged : source.paged()) {
            ReindexResult partial = runPaged(paged, source);
            indexed += partial.indexed();
            failed += partial.failed();
        }
        log.info("Reindex {} TERMINADO: {} indexados, {} fallidos", entity, indexed, failed);
        progress.entityDone(entity, entity, indexed, failed);
        return new ReindexResult(entity, indexed, failed);
    }

    /**
     * Reindexa UN registro. Se pide su payload a la fuente y se sobrescribe el
     * documento; si la fuente ya no lo tiene, el fallo se propaga como 400 con
     * el motivo, no como un 500 mudo.
     */
    public ReindexResult reindexOne(String entity, String id) {
        EntitySource source = require(entity);
        JsonNode payload;
        try {
            payload = source.one().apply(id);
        } catch (Exception ex) {
            throw new BusinessException(
                    "No se pudo leer el registro " + id + " de " + entity + ": " + ex.getMessage());
        }
        if (payload == null || payload.isNull()) {
            throw new BusinessException("El registro " + id + " ya no existe en " + entity);
        }
        boolean ok = index(payload, source, entity, Instant.now());
        return new ReindexResult(entity, ok ? 1 : 0, ok ? 0 : 1);
    }

    /**
     * Foto de cada indice para la pantalla de gestion. Si la fuente esta caida
     * se informa ({@code sourceUp=false}) en vez de fallar la pantalla entera.
     */
    public List<IndexStatus> status() {
        List<IndexStatus> out = new ArrayList<>();
        sources().forEach((entity, source) -> {
            Long total = null;
            boolean up = true;
            try {
                long sum = 0;
                for (PagedSource paged : source.paged()) sum += paged.count().get();
                total = sum;
            } catch (Exception ex) {
                up = false;
                log.warn("Estado de {}: la fuente no responde ({})", entity, ex.getMessage());
            }
            out.add(new IndexStatus(entity, source.alias(), total, countInElastic(source), up));
        });
        return out;
    }

    // ==================== ARRANQUE ====================

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)  // ← debe correr DESPUES que IndexBootstrap haya creado indices/aliases
    public void reindexOnStartup() {
        if (!reindexEnabled) {
            log.info("Reindex en startup DESACTIVADO (saas.search.reindex.enabled=false)");
            return;
        }
        // Va por la MISMA cola que los reindex manuales: si no, el de arranque y
        // uno lanzado desde la pantalla corren a la vez, se pisan contra las
        // fuentes y el log en vivo mezcla eventos de los dos. De paso deja de
        // bloquear el hilo del ApplicationReadyEvent durante la espera a Eureka.
        reindexPool.submit(this::runStartupReindex);
    }

    private void runStartupReindex() {
        Set<String> entities = parseEntities(entitiesProperty);
        log.info("Reindex INICIADO. Entidades: {}", entities);

        // Esperar a que Eureka tenga registradas las instancias necesarias.
        // Sin esto, search-service arranca antes que su DiscoveryClient haga
        // el primer fetch del registry (~30s) y los Feign calls fallan con
        // "No servers available for service: ...".
        Map<String, EntitySource> sources = sources();
        Set<String> requiredServices = entities.stream()
                .map(sources::get)
                .filter(Objects::nonNull)
                .map(EntitySource::service)
                .collect(Collectors.toSet());
        if (!waitForServices(requiredServices)) {
            log.warn("Reindex ABORTADO: timeout esperando services en Eureka. " +
                    "Relanza desde la gestion de Elastic (POST /search/admin/reindex) " +
                    "cuando las fuentes esten arriba.");
            return;
        }

        long startMillis = System.currentTimeMillis();
        long indexed = 0;
        long failed = 0;
        progress.reset();
        progress.start("arranque");
        for (String entity : entities) {
            if (!sources.containsKey(entity)) {
                log.warn("Entidad desconocida en reindex: '{}' (ignorada)", entity);
                continue;
            }
            try {
                ReindexResult result = reindexEntity(entity);
                indexed += result.indexed();
                failed += result.failed();
            } catch (Exception ex) {
                log.error("Reindex de {} fallo: {}", entity, ex.getMessage(), ex);
                failed++;
            }
        }
        progress.end("arranque", indexed, failed, System.currentTimeMillis() - startMillis);
        log.info("Reindex FINALIZADO en {} ms", System.currentTimeMillis() - startMillis);
    }

    // ==================== CATALOGO DE ENTIDADES ====================

    /**
     * Como se reindexa una entidad. {@code paged} es una lista porque
     * {@code locations} tiene cuatro fuentes en MySQL (pais, departamento,
     * municipio, barrio) que vuelcan al MISMO alias de Elastic.
     */
    private record EntitySource(
            String alias,
            String service,
            Class<? extends BaseDocument> documentClass,
            List<PagedSource> paged,
            Function<String, JsonNode> one) {}

    private record PagedSource(
            String label,
            Supplier<Long> count,
            BiFunction<Integer, Integer, List<JsonNode>> fetch) {}

    private Map<String, EntitySource> sources() {
        Map<String, EntitySource> map = new LinkedHashMap<>();

        map.put(Entities.USER_ENTITY, new EntitySource(
                indexNames.users(), "auth-service", UserDocument.class,
                List.of(new PagedSource("users",
                        () -> authClient.countUsers().getOrDefault("total", 0L),
                        authClient::fetchUsers)),
                authClient::fetchUser));

        map.put(Entities.LOCATION_ENTITY, new EntitySource(
                indexNames.locations(), "system-service", LocationDocument.class,
                List.of(
                        new PagedSource("locations:countries",
                                () -> systemClient.countCountries().getOrDefault("total", 0L),
                                systemClient::fetchCountries),
                        new PagedSource("locations:departments",
                                () -> systemClient.countDepartments().getOrDefault("total", 0L),
                                systemClient::fetchDepartments),
                        new PagedSource("locations:municipalities",
                                () -> systemClient.countMunicipalities().getOrDefault("total", 0L),
                                systemClient::fetchMunicipalities),
                        new PagedSource("locations:neighborhoods",
                                () -> systemClient.countNeighborhoods().getOrDefault("total", 0L),
                                systemClient::fetchNeighborhoods)),
                systemClient::fetchLocation));

        map.put(Entities.THIRDPARTY_ENTITY, new EntitySource(
                indexNames.thirdParties(), "thirdparty-service", ThirdPartyDocument.class,
                List.of(new PagedSource("third_parties",
                        () -> thirdpartyClient.countThirdParties().getOrDefault("total", 0L),
                        thirdpartyClient::fetchThirdParties)),
                thirdpartyClient::fetchThirdParty));

        map.put(Entities.EMPLOYEE_BALANCE_ENTITY, new EntitySource(
                indexNames.employeeBalances(), "finance-service", EmployeeBalanceDocument.class,
                List.of(new PagedSource("employee_balances",
                        () -> financeClient.countBalances().getOrDefault("total", 0L),
                        financeClient::fetchBalances)),
                financeClient::fetchBalance));

        return map;
    }

    private EntitySource require(String entity) {
        EntitySource source = sources().get(entity == null ? null : entity.trim().toLowerCase());
        if (source == null) {
            throw new BusinessException("Entidad no indexable: '" + entity + "'. Disponibles: " + supportedEntities());
        }
        return source;
    }

    // ==================== PIPELINE ====================

    /** Recorre una fuente paginada completa y la vuelca al alias de la entidad. */
    private ReindexResult runPaged(PagedSource source, EntitySource entity) {
        long total;
        try {
            total = source.count().get();
        } catch (Exception ex) {
            log.error("Reindex {}: no se pudo obtener total (servicio caido?). Aborta. err={}",
                    source.label(), ex.getMessage());
            return new ReindexResult(source.label(), 0, 0);
        }

        if (total == 0) {
            log.info("Reindex {}: 0 registros, nada que hacer", source.label());
            return new ReindexResult(source.label(), 0, 0);
        }

        log.info("Reindex {}: {} registros a indexar", source.label(), total);
        // Ancla el paso en 0 en cuanto se sabe su total. Sin esto la barra se
        // queda en el 100% del paso ANTERIOR mientras se pide la primera pagina
        // de este, y parece terminado cuando ni ha empezado.
        progress.progress(entity.alias(), source.label(), 0, total, 0);

        Instant now = Instant.now();
        long indexed = 0;
        long failed = 0;
        int page = 0;

        while (true) {
            List<JsonNode> batch = source.fetch().apply(page, PAGE_SIZE);
            if (batch == null || batch.isEmpty()) break;

            for (JsonNode payload : batch) {
                if (index(payload, entity, source.label(), now)) indexed++;
                else failed++;
                // Avisa DENTRO de la pagina, no solo al terminarla: una pagina de
                // usuarios tarda casi un minuto (una llamada al origen por cuenta)
                // y con un solo aviso al final la barra parece congelada.
                if ((indexed + failed) % PROGRESS_EVERY == 0) {
                    progress.progress(entity.alias(), source.label(), indexed, total, failed);
                }
            }

            log.info("Reindex {}: {}/{} indexados", source.label(), indexed, total);
            progress.progress(entity.alias(), source.label(), indexed, total, failed);

            if (batch.size() < PAGE_SIZE) break;
            page++;
        }
        return new ReindexResult(source.label(), indexed, failed);
    }

    /** Mapea un payload de la fuente a documento y lo guarda. false si falla. */
    private boolean index(JsonNode payload, EntitySource entity, String label, Instant now) {
        try {
            BaseDocument doc = mapper.treeToValue(payload, entity.documentClass());
            UUID id = UUID.fromString(payload.get("id").asText());
            doc.setId(id.toString());
            doc.setUpdatedAt(now);
            if (doc.getCreatedAt() == null) doc.setCreatedAt(now);
            doc.setDocVersion(now.toEpochMilli());
            ops.save(doc, IndexCoordinates.of(entity.alias()));
            return true;
        } catch (Exception ex) {
            String id = payload.has("id") ? payload.get("id").asText() : "?";
            log.warn("Reindex {}: fallo id={} err={} (continuando)", label, id, ex.getMessage());
            return false;
        }
    }

    private long countInElastic(EntitySource source) {
        try {
            return ops.count(Query.findAll(), source.documentClass(), IndexCoordinates.of(source.alias()));
        } catch (Exception ex) {
            log.warn("No se pudo contar el indice {}: {}", source.alias(), ex.getMessage());
            return 0;
        }
    }

    private Set<String> parseEntities(String prop) {
        if (prop == null || prop.isBlank() || "all".equalsIgnoreCase(prop.trim())) {
            return sources().keySet();
        }
        return Arrays.stream(prop.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Bloquea hasta que todas las instancias requeridas esten registradas en
     * Eureka, o hasta {@link #EUREKA_WAIT_TIMEOUT_MS}.
     *
     * <p>Necesario porque el reindex se dispara con {@code ApplicationReadyEvent}
     * (justo al terminar de arrancar), pero el {@code DiscoveryClient} tarda
     * varios segundos en hacer su primer fetch del registry.
     *
     * @return true si todos los services estan disponibles, false si hubo timeout.
     */
    private boolean waitForServices(Set<String> services) {
        if (services.isEmpty()) return true;

        long deadline = System.currentTimeMillis() + EUREKA_WAIT_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            Set<String> missing = services.stream()
                    .filter(s -> discoveryClient.getInstances(s).isEmpty())
                    .collect(Collectors.toSet());

            if (missing.isEmpty()) {
                log.info("Eureka: todos los services requeridos disponibles {}", services);
                return true;
            }

            log.info("Eureka: esperando services {} (faltan {})...", services, missing);
            try {
                Thread.sleep(EUREKA_WAIT_POLL_MS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
