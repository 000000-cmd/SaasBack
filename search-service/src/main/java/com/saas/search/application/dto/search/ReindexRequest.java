package com.saas.search.application.dto.search;

/**
 * Cuerpo de un reindex manual. La granularidad sale de que campos vengan, asi
 * las tres operaciones comparten un unico endpoint:
 *
 * <pre>
 *   {}                                     -> todo Elastic
 *   { "entity": "third_parties" }          -> todos los terceros
 *   { "entity": "third_parties", "id": .. } -> ese tercero
 * </pre>
 */
public record ReindexRequest(String entity, String id) {

    public boolean hasEntity() { return entity != null && !entity.isBlank(); }

    public boolean hasId() { return id != null && !id.isBlank(); }
}
