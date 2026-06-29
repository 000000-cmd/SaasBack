package com.saas.search.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Crea indices versionados y aliases al arranque, si no existen.
 *
 * Estrategia "alias-versioned":
 *
 *   Indice fisico: {@code users_v1}, {@code roles_v1}.
 *   Alias logico: {@code users}, {@code roles}.
 *   El codigo solo conoce los aliases.
 *   Reindex futuro: crear {@code users_v2}, copiar, swap del alias, drop v1.
 *
 * Idempotente: corre en cada arranque, no rompe nada si ya existe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexBootstrap {
    private final ElasticsearchClient client;

    /** Especificacion declarativa de los indices a crear. */
    private static final List<IndexSpec> SPECS = List.of(
            new IndexSpec("users",     "users_v1",     "elasticsearch/users-mapping.json"),
            new IndexSpec("roles",     "roles_v1",     "elasticsearch/roles-mapping.json"),
            new IndexSpec("locations", "locations_v1", "elasticsearch/locations-mapping.json"),
            new IndexSpec("third_parties", "third_parties_v1", "elasticsearch/thirdparties-mapping.json")
    );

    /**
     * Se dispara al final del arranque (despues de que todos los beans estan listos).
     * Usar {@code ApplicationReadyEvent} en lugar de {@code @PostConstruct} para
     * que cualquier dependencia diferida tambien este arriba.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)   // ← debe correr ANTES que ReindexService
    public void boostrap() {
        log.info("IndexBootstrap: verificando indices y aliases...");
        for (IndexSpec spec : SPECS){
            try {
                createIfNotExist(spec);
                ensureAlias(spec);

            } catch (Exception ex) {
                log.error("IndexBootstrap: fallo procesando {}: {}",
                        spec.indexName, ex.getMessage(), ex);
            }
        }
        log.info("IndexBootstrap: completado.");
    }

    private void createIfNotExist(IndexSpec spec) throws Exception {
        boolean exists = client.indices()
                .exists(b -> b.index(spec.indexName))
                .value();

        if (exists) {
            log.debug("Indice {} ya existe, skip", spec.indexName);
            return;
        }

        try (InputStream is = new ClassPathResource(spec.mappingPath).getInputStream()) {
            CreateIndexRequest req = CreateIndexRequest.of(b -> b
                    .index(spec.indexName)
                    .withJson(is));

            client.indices().create(req);

            log.info("Indice {} creado con mapping de {}", spec.indexName, spec.mappingPath);
        }
    }

    private void ensureAlias(IndexSpec spec) throws Exception {
        boolean aliasExist = client.indices()
                .existsAlias(b -> b.name(spec.alias))
                .value();

        if (aliasExist) {
            log.debug("Alias {} ya existe, skip", spec.alias);
            return;
        }

        client.indices().updateAliases(UpdateAliasesRequest.of( b -> b
                                                                            .actions(Action.of(a -> a.add(add -> add
                                                                                                                                .index(spec.indexName)
                                                                                                                                .alias(spec.alias))))));
        log.info("Alias {} -> {} creado", spec.alias, spec.indexName);

    }

    private record IndexSpec(String alias, String indexName, String mappingPath){};


}
