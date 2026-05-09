package com.saas.search.application.dto.search;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Criterios de busqueda genericos que el {@link com.saas.search.application.service.search.BaseSearchService}
 * traduce a queries de Elasticsearch.
 *
 * <p>Estructura tres conceptos distintos:
 * <ul>
 *   <li><b>query</b>: texto libre full-text. Se aplica via {@code multi_match}
 *       contra los campos declarados por cada subclase de {@code BaseSearchService}.</li>
 *   <li><b>filters</b>: filtros estructurados (igualdad exacta o IN). Se traducen
 *       a {@code term} o {@code terms} queries.</li>
 *   <li><b>businessId</b>: multi-tenancy. Cuando esta presente se aplica como
 *       filtro implicito SIEMPRE, garantizando aislamiento por negocio.</li>
 * </ul>
 */
@Data
@Builder
public class SearchCriteria {

    /** Texto libre. Si null o vacio, no aplica filtro full-text. */
    private String query;

    /**
     * Filtros estructurados: campo -> valor.
     * El valor puede ser:
     * <ul>
     *   <li>Un objeto simple (String, Boolean, Number) -> {@code term} query.</li>
     *   <li>Una {@code Collection} -> {@code terms} query (matchea cualquiera = OR).</li>
     * </ul>
     */
    @Builder.Default
    private Map<String, Object> filters = new HashMap<>();

    /** Multi-tenancy. Aplica como filtro obligatorio cuando no es null. */
    private UUID businessId;
}
