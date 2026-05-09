package com.saas.search.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.search.domain.document.BaseDocument;
import com.saas.search.domain.document.RoleDocument;
import com.saas.search.domain.document.UserDocument;
import com.saas.search.domain.constants.Entities;
import com.saas.search.infrastructure.client.AuthInternalClient;
import com.saas.search.infrastructure.client.SystemInternalClient;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Reindex completo desde fuentes (auth, system) hacia Elasticsearch.
 *
 * Se dispara al arrancar si {@code saas.search.reindex.enabled=true}.
 *
 * {@code saas.search.reindex.entities} controla que se reindexa:
 *
 *   all(default): reindexa todas las entidades soportadas.
 *   users,roles: lista separada por coma de entidades especificas.
 *
 *
 * Estrategia: paginado por 500 registros. Cada batch se trae via Feign
 * y se indexa en bulk en ES.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReindexService {

    private static final int PAGE_SIZE = 500;
    private static final Set<String> ALL_ENTITIES = Set.of(
            Entities.ROLE_ENTITY,
            Entities.USER_ENTITY
    );

    /** Mapeo entidad -> nombre del service Eureka que la provee. */
    private static final java.util.Map<String, String> ENTITY_TO_SERVICE = java.util.Map.of(
            Entities.USER_ENTITY, "auth-service",
            Entities.ROLE_ENTITY, "system-service"
    );

    /** Tiempo maximo a esperar a que Eureka tenga las instancias registradas. */
    private static final long EUREKA_WAIT_TIMEOUT_MS = 60_000;
    private static final long EUREKA_WAIT_POLL_MS    = 2_000;

    private final AuthInternalClient authClient;
    private final SystemInternalClient systemClient;
    private final ElasticsearchOperations ops;
    private final ObjectMapper mapper;
    private final IndexNames indexNames;
    private final DiscoveryClient discoveryClient;

    @Value("${saas.search.reindex.enabled:false}")
    private boolean reindexEnabled;

    @Value("${saas.search.reindex.entities:all}")
    private String entitiesProperty;

    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    public void reindexOnStartup() {
        if (!reindexEnabled) {
            log.info("Reindex en startup DESACTIVADO (saas.search.reindex.enabled=false)");
            return;
        }

        Set<String> entities = parseEntities(entitiesProperty);
        log.info("Reindex INICIADO. Entidades: {}", entities);

        // Esperar a que Eureka tenga registradas las instancias necesarias.
        // Sin esto, search-service arranca antes que su DiscoveryClient haga
        // el primer fetch del registry (~30s) y los Feign calls fallan con
        // "No servers available for service: ...".
        Set<String> requiredServices = entities.stream()
                .map(ENTITY_TO_SERVICE::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (!waitForServices(requiredServices)) {
            log.warn("Reindex ABORTADO: timeout esperando services en Eureka. " +
                    "Activa el reindex manual con un POST al endpoint admin (futuro) " +
                    "o reinicia search-service cuando auth/system esten arriba.");
            return;
        }

        long startMillis = System.currentTimeMillis();

        for (String entity : entities) {
            try {
                switch (entity) {
                    case Entities.USER_ENTITY -> reindex(
                            Entities.USER_ENTITY,
                            indexNames.users(),
                            () -> authClient.countUsers().getOrDefault("total", 0L),
                            authClient::fetchUsers,
                            UserDocument.class);
                    case Entities.ROLE_ENTITY -> reindex(
                            Entities.ROLE_ENTITY,
                            indexNames.roles(),
                            () -> systemClient.countRoles().getOrDefault("total", 0L),
                            systemClient::fetchRoles,
                            RoleDocument.class);
                    default -> log.warn("Entidad desconocida en reindex: '{}' (ignorada)", entity);
                }
            } catch (Exception ex) {
                log.error("Reindex de {} fallo: {}", entity, ex.getMessage(), ex);
            }
        }

        log.info("Reindex FINALIZADO en {} ms", System.currentTimeMillis() - startMillis);
    }

    /**
     * Pipeline generico de reindex paginado para cualquier entidad.
     *
     * @param entityLabel       texto para logs ("users", "roles")
     * @param indexAlias        alias destino en ES
     * @param countFetcher      como obtener el total a indexar (Feign /count)
     * @param batchFetcher      como obtener una pagina (Feign /all?page,size)
     * @param documentClass     clase del documento ES al que mapear
     */
    private <D extends BaseDocument> void reindex(
            String entityLabel,
            String indexAlias,
            Supplier<Long> countFetcher,
            BiFunction<Integer, Integer, List<JsonNode>> batchFetcher,
            Class<D> documentClass) {

        long total;
        try {
            total = countFetcher.get();
        } catch (Exception ex) {
            log.error("Reindex {}: no se pudo obtener total (servicio caido?). Aborta. err={}",
                    entityLabel, ex.getMessage());
            return;
        }

        if (total == 0) {
            log.info("Reindex {}: 0 registros, nada que hacer", entityLabel);
            return;
        }

        log.info("Reindex {}: {} registros a indexar", entityLabel, total);

        IndexCoordinates index = IndexCoordinates.of(indexAlias);
        Instant now = Instant.now();
        long indexed = 0;
        long failed = 0;
        int page = 0;

        while (true) {
            List<JsonNode> batch = batchFetcher.apply(page, PAGE_SIZE);
            if (batch == null || batch.isEmpty()) break;

            for (JsonNode payload : batch) {
                try {
                    D doc = mapper.treeToValue(payload, documentClass);
                    UUID id = UUID.fromString(payload.get("id").asText());
                    doc.setId(id.toString());
                    doc.setUpdatedAt(now);
                    if (doc.getCreatedAt() == null) {
                        doc.setCreatedAt(now);
                    }
                    doc.setDocVersion(now.toEpochMilli());
                    ops.save(doc, index);
                    indexed++;
                } catch (Exception ex) {
                    failed++;
                    String id = payload.has("id") ? payload.get("id").asText() : "?";
                    log.warn("Reindex {}: fallo id={} err={} (continuando)",
                            entityLabel, id, ex.getMessage());
                }
            }

            log.info("Reindex {}: {}/{} indexados", entityLabel, indexed, total);

            if (batch.size() < PAGE_SIZE) break;
            page++;
        }

        if (failed > 0) {
            log.warn("Reindex {} TERMINADO: {} indexados, {} fallidos (revisar warnings)",
                    entityLabel, indexed, failed);
        } else {
            log.info("Reindex {} TERMINADO: {} indexados, sin fallos", entityLabel, indexed);
        }
    }

    private Set<String> parseEntities(String prop) {
        if (prop == null || prop.isBlank() || "all".equalsIgnoreCase(prop.trim())) {
            return ALL_ENTITIES;
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
