package com.saas.search.application.service.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.saas.search.application.dto.search.SearchCriteria;
import com.saas.search.application.dto.search.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Servicio base para busquedas en Elasticsearch.
 *
 * <p>Subclases concretas (UserSearchService, RoleSearchService) declaran:
 * <ul>
 *   <li>{@link #documentClass()}: la clase del documento a deserializar</li>
 *   <li>{@link #aliasName()}: el alias ES (no el indice fisico)</li>
 *   <li>{@link #searchableFields()}: campos donde aplica el query full-text</li>
 * </ul>
 *
 * <p>Construye una {@code bool query} con tres bloques:
 * <ul>
 *   <li><b>filter (businessId)</b>: multi-tenancy obligatorio si esta presente.</li>
 *   <li><b>must (multi_match)</b>: full-text contra los campos searchable.</li>
 *   <li><b>filter (term/terms)</b>: filtros estructurados.</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseSearchService<D> {

    protected final ElasticsearchOperations ops;

    /** La clase del documento ES. ES la usa para deserializar resultados. */
    protected abstract Class<D> documentClass();

    /** Alias del indice (ej: "users"). Permite reindex zero-downtime. */
    protected abstract String aliasName();

    /**
     * Campos donde aplica el {@code query} libre.
     * Sintaxis: {@code "campo"} o {@code "campo^N"} para boost de relevancia.
     * Ej: {@code List.of("username^3", "email^2", "fullName")}.
     */
    protected abstract List<String> searchableFields();

    /**
     * Ejecuta una busqueda y devuelve resultados paginados.
     *
     * @param criteria   query libre + filtros + businessId
     * @param page       indice de pagina (0-based)
     * @param size       elementos por pagina
     * @param sortField  campo por el cual ordenar (null = orden por relevancia)
     * @param sortDir    "asc" o "desc"
     */
    public SearchResponse<D> search(SearchCriteria criteria,
                                    int page,
                                    int size,
                                    String sortField,
                                    String sortDir) {

        BoolQuery.Builder bool = QueryBuilders.bool();

        // 1. MULTI-TENANCY (filtro implicito obligatorio cuando hay businessId).
        if (criteria.getBusinessId() != null) {
            bool.filter(f -> f.term(t -> t
                    .field("businessId")
                    .value(criteria.getBusinessId().toString())));
        }

        // 2. FULL-TEXT QUERY (must = afecta el score / relevancia).
        if (criteria.getQuery() != null && !criteria.getQuery().isBlank()) {
            bool.must(m -> m.multiMatch(mm -> mm
                    .query(criteria.getQuery().trim())
                    .fields(searchableFields())
                    .fuzziness("AUTO")));
        } else {
            // Sin query: match_all (todos los docs que pasen los filtros)
            bool.must(m -> m.matchAll(ma -> ma));
        }

        // 3. FILTROS ESTRUCTURADOS (filter = no afecta el score).
        if (criteria.getFilters() != null) {
            criteria.getFilters().forEach((field, value) -> {
                if (value != null) addFilter(bool, field, value);
            });
        }

        // 4. CONSTRUIR LA QUERY con paginacion + sort.
        var queryBuilder = NativeQuery.builder()
                .withQuery(bool.build()._toQuery())
                .withPageable(PageRequest.of(page, size));

        if (sortField != null && !sortField.isBlank()) {
            SortOrder order = "desc".equalsIgnoreCase(sortDir) ? SortOrder.Desc : SortOrder.Asc;
            queryBuilder.withSort(SortOptions.of(s -> s.field(f -> f.field(sortField).order(order))));
        }

        // 5. EJECUTAR contra el alias y mapear resultados.
        SearchHits<D> hits = ops.search(queryBuilder.build(), documentClass(), IndexCoordinates.of(aliasName()));

        List<D> items = hits.stream().map(SearchHit::getContent).toList();

        log.debug("Search en {}: q='{}', filters={}, total={}, page={}/{}",
                aliasName(), criteria.getQuery(), criteria.getFilters(),
                hits.getTotalHits(), page, size);

        return SearchResponse.of(items, hits.getTotalHits(), page, size);
    }

    /**
     * Agrega un filtro al bool query:
     * <ul>
     *   <li>{@code Collection} -> {@code terms} (matchea CUALQUIERA, equivale a OR).</li>
     *   <li>Otro tipo -> {@code term} (igualdad exacta).</li>
     * </ul>
     */
    private void addFilter(BoolQuery.Builder bool, String field, Object value) {
        if (value instanceof Collection<?> col) {
            if (col.isEmpty()) return;
            List<FieldValue> values = col.stream()
                    .map(v -> FieldValue.of(String.valueOf(v)))
                    .toList();
            bool.filter(f -> f.terms(t -> t
                    .field(field)
                    .terms(tv -> tv.value(values))));
        } else {
            bool.filter(f -> f.term(t -> t
                    .field(field)
                    .value(String.valueOf(value))));
        }
    }
}
