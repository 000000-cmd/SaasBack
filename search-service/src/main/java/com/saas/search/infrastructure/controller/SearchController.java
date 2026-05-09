package com.saas.search.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.search.application.dto.search.SearchCriteria;
import com.saas.search.application.dto.search.SearchResponse;
import com.saas.search.application.service.search.RoleSearchService;
import com.saas.search.application.service.search.UserSearchService;
import com.saas.search.domain.document.RoleDocument;
import com.saas.search.domain.document.UserDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints publicos de busqueda en Elasticsearch.
 *
 * <p>El context-path es {@code /search}, por eso este controller no anade
 * prefijo. Los endpoints quedan:
 * <ul>
 *   <li>{@code GET /search/users}</li>
 *   <li>{@code GET /search/roles}</li>
 * </ul>
 *
 * <p>Convencion de query params:
 * <ul>
 *   <li>{@code q}: texto libre (full-text). Opcional.</li>
 *   <li>{@code page} (default 0), {@code size} (default 20).</li>
 *   <li>{@code sort}: formato {@code campo,dir}. Ej: {@code sort=fullName,asc}.</li>
 *   <li>{@code businessId}: opcional; en produccion vendra del JWT.</li>
 *   <li>Filtros especificos: {@code roleCodes}, {@code enabled}.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final UserSearchService userSearch;
    private final RoleSearchService roleSearch;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<SearchResponse<UserDocument>>> searchUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> roleCodes,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) UUID businessId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {

        Map<String, Object> filters = new HashMap<>();
        if (roleCodes != null && !roleCodes.isEmpty()) filters.put("roleCodes", roleCodes);
        if (enabled != null) filters.put("enabled", enabled);

        SearchCriteria criteria = SearchCriteria.builder()
                .query(q)
                .filters(filters)
                .businessId(businessId)
                .build();

        String[] sortParts = parseSort(sort);
        SearchResponse<UserDocument> result = userSearch.search(
                criteria, page, size, sortParts[0], sortParts[1]);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<SearchResponse<RoleDocument>>> searchRoles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) UUID businessId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {

        Map<String, Object> filters = new HashMap<>();
        if (enabled != null) filters.put("enabled", enabled);

        SearchCriteria criteria = SearchCriteria.builder()
                .query(q)
                .filters(filters)
                .businessId(businessId)
                .build();

        String[] sortParts = parseSort(sort);
        SearchResponse<RoleDocument> result = roleSearch.search(
                criteria, page, size, sortParts[0], sortParts[1]);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Parsea el query param {@code sort=campo,dir}.
     * Si sort es null/vacio, retorna {@code [null, "asc"]} (sort por relevancia ES).
     * Si solo trae el campo (sin coma), asume {@code asc}.
     */
    private String[] parseSort(String sort) {
        if (sort == null || sort.isBlank()) return new String[]{null, "asc"};
        String[] parts = sort.split(",");
        return new String[]{
                parts[0].trim(),
                parts.length > 1 ? parts[1].trim() : "asc"
        };
    }
}
