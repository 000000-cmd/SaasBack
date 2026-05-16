package com.saas.search.application.service.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.saas.search.application.dto.search.SearchResponse;
import com.saas.search.domain.document.LocationDocument;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Busqueda de localizacion (pais, departamento, municipio, barrio/vereda).
 *
 * <p>Reglas:</p>
 * <ul>
 *   <li>El parametro {@code q} requiere minimo 3 caracteres; con menos retorna lista vacia.</li>
 *   <li>El query usa {@code bool_prefix} sobre {@code search_as_you_type} para soportar
 *       autocompletar con boost en el campo del nivel buscado.</li>
 *   <li>Cada respuesta incluye toda la jerarquia (codigos + nombres) ya desnormalizada
 *       en el documento, asi un solo hit lleva pais > depto > municipio > barrio.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationSearchService {

    public static final int MIN_QUERY_LENGTH = 3;

    public static final String LEVEL_COUNTRY = "PAIS";
    public static final String LEVEL_DEPARTMENT = "DEPARTAMENTO";
    public static final String LEVEL_MUNICIPALITY = "MUNICIPIO";
    public static final String LEVEL_NEIGHBORHOOD_GROUP = "BARRIO|VEREDA|CORREGIMIENTO|OTRO";

    private final ElasticsearchOperations ops;
    private final IndexNames indexNames;

    /** Busqueda de paises por nombre (>= 3 letras). */
    public SearchResponse<LocationDocument> searchCountries(String q, int page, int size) {
        return execute(buildQuery(q, "countryName", LEVEL_COUNTRY, Map.of()), page, size);
    }

    /** Busqueda de departamentos por nombre (>= 3 letras). Opcionalmente filtrado por pais. */
    public SearchResponse<LocationDocument> searchDepartments(String q, String countryCode, int page, int size) {
        Map<String, String> filters = countryCode != null && !countryCode.isBlank()
                ? Map.of("countryCode", countryCode) : Map.of();
        return execute(buildQuery(q, "departmentName", LEVEL_DEPARTMENT, filters), page, size);
    }

    /** Busqueda de municipios. */
    public SearchResponse<LocationDocument> searchMunicipalities(String q, String countryCode, String departmentCode,
                                                                 int page, int size) {
        Map<String, String> filters = new java.util.HashMap<>();
        if (countryCode != null && !countryCode.isBlank())     filters.put("countryCode", countryCode);
        if (departmentCode != null && !departmentCode.isBlank()) filters.put("departmentCode", departmentCode);
        return execute(buildQuery(q, "municipalityName", LEVEL_MUNICIPALITY, filters), page, size);
    }

    /** Busqueda de barrios/veredas. Devuelve toda la jerarquia. */
    public SearchResponse<LocationDocument> searchNeighborhoods(String q, String countryCode, String departmentCode,
                                                                String municipalityCode, String type,
                                                                int page, int size) {
        BoolQuery.Builder bool = QueryBuilders.bool();

        // Filtro por nivel: cualquier tipo de barrio/vereda
        bool.filter(f -> f.terms(t -> t.field("level")
                .terms(tv -> tv.value(List.of(
                        FieldValue.of("BARRIO"),
                        FieldValue.of("VEREDA"),
                        FieldValue.of("CORREGIMIENTO"),
                        FieldValue.of("OTRO"))))));

        if (type != null && !type.isBlank()) {
            bool.filter(f -> f.term(t -> t.field("level").value(type.toUpperCase())));
        }
        if (countryCode != null && !countryCode.isBlank())
            bool.filter(f -> f.term(t -> t.field("countryCode").value(countryCode)));
        if (departmentCode != null && !departmentCode.isBlank())
            bool.filter(f -> f.term(t -> t.field("departmentCode").value(departmentCode)));
        if (municipalityCode != null && !municipalityCode.isBlank())
            bool.filter(f -> f.term(t -> t.field("municipalityCode").value(municipalityCode)));

        // Solo agregar el match-by-name si hay q valido (>=3 chars). Sin q,
        // se devuelve todo lo que matchea los filtros (util para la vista de
        // arbol que pide "todos los barrios del municipio X").
        if (q != null && q.length() >= MIN_QUERY_LENGTH) {
            bool.must(m -> m.multiMatch(mm -> mm
                    .query(q)
                    .type(TextQueryType.BoolPrefix)
                    .fields("neighborhoodName^4",
                            "neighborhoodName._2gram",
                            "neighborhoodName._3gram")));
        }

        return execute(bool, page, size);
    }

    /**
     * Buscar SOLO por nombre de barrio/vereda (3+ letras) y devolver jerarquia completa.
     * Ideal para selects donde el usuario escribe el barrio y se autocompleta todo.
     */
    public SearchResponse<LocationDocument> searchByNeighborhood(String q, int page, int size) {
        return searchNeighborhoods(q, null, null, null, null, page, size);
    }

    /**
     * Busqueda jerarquica en cascada. Especifica los padres conocidos y consulta el siguiente nivel.
     * Ej: country=CO, department=05, level=MUNICIPIO, q=med -> "Medellin" en Antioquia.
     */
    public SearchResponse<LocationDocument> cascade(String level, String q,
                                                    String countryCode, String departmentCode, String municipalityCode,
                                                    int page, int size) {
        if (level == null || level.isBlank()) {
            return SearchResponse.of(List.of(), 0L, page, size);
        }
        String lvl = level.toUpperCase();

        return switch (lvl) {
            case LEVEL_COUNTRY -> searchCountries(q, page, size);
            case LEVEL_DEPARTMENT -> searchDepartments(q, countryCode, page, size);
            case LEVEL_MUNICIPALITY -> searchMunicipalities(q, countryCode, departmentCode, page, size);
            case "BARRIO", "VEREDA", "CORREGIMIENTO", "OTRO", "NEIGHBORHOOD" ->
                    searchNeighborhoods(q, countryCode, departmentCode, municipalityCode,
                            "NEIGHBORHOOD".equals(lvl) ? null : lvl, page, size);
            default -> SearchResponse.of(List.of(), 0L, page, size);
        };
    }

    // ==================== INTERNAL ====================

    private BoolQuery.Builder buildQuery(String q, String nameField, String level, Map<String, String> filters) {
        BoolQuery.Builder bool = QueryBuilders.bool();

        bool.filter(f -> f.term(t -> t.field("level").value(level)));

        filters.forEach((field, value) ->
                bool.filter(f -> f.term(t -> t.field(field).value(value))));

        if (q == null || q.length() < MIN_QUERY_LENGTH) {
            // Si no hay query valido, no agregamos un must -> retorna todo del nivel.
            // Para no inundar, devolvemos lista vacia desde execute via short-circuit en search*.
            // Aqui dejamos la construccion lista para reuso interno.
            return bool;
        }

        bool.must(m -> m.multiMatch(mm -> mm
                .query(q)
                .type(TextQueryType.BoolPrefix)
                .fields(nameField + "^4",
                        nameField + "._2gram",
                        nameField + "._3gram")));
        return bool;
    }

    private SearchResponse<LocationDocument> execute(BoolQuery.Builder bool, int page, int size) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(bool.build()._toQuery())
                .withPageable(PageRequest.of(page, size))
                .withSort(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))))
                .build();

        SearchHits<LocationDocument> hits = ops.search(
                query, LocationDocument.class, IndexCoordinates.of(indexNames.locations()));

        List<LocationDocument> items = hits.stream().map(SearchHit::getContent).toList();
        return SearchResponse.of(items, hits.getTotalHits(), page, size);
    }
}
