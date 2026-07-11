package com.saas.search.application.dto.search;

import java.util.List;
import java.util.UUID;

/**
 * Cuerpo (POST) de una búsqueda en Elasticsearch. Reemplaza el envío de filtros
 * y paginación por query params para que las peticiones sean más limpias.
 * Todos los campos son opcionales; la paginación tiene defaults.
 *
 *   { "q": "ana", "enabled": true, "page": 0, "size": 20, "sort": "fullName,asc" }
 */
public record SearchRequest(
        String q,
        List<String> roleCodes,
        Boolean enabled,
        UUID businessId,
        Integer page,
        Integer size,
        String sort
) {
    public int pageOrDefault() { return page == null || page < 0 ? 0 : page; }
    public int sizeOrDefault() { return size == null || size <= 0 ? 20 : size; }
}
