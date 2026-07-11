package com.saas.search.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.search.application.dto.search.SearchCriteria;
import com.saas.search.application.dto.search.SearchRequest;
import com.saas.search.application.dto.search.SearchResponse;
import com.saas.search.application.service.search.RoleSearchService;
import com.saas.search.application.service.search.ThirdPartySearchService;
import com.saas.search.application.service.search.UserSearchService;
import com.saas.search.domain.document.RoleDocument;
import com.saas.search.domain.document.ThirdPartyDocument;
import com.saas.search.domain.document.UserDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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
    private final ThirdPartySearchService thirdPartySearch;

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<SearchResponse<UserDocument>>> searchUsers(@RequestBody SearchRequest req) {
        Map<String, Object> filters = new HashMap<>();
        if (req.roleCodes() != null && !req.roleCodes().isEmpty()) filters.put("roleCodes", req.roleCodes());
        if (req.enabled() != null) filters.put("enabled", req.enabled());

        SearchCriteria criteria = SearchCriteria.builder()
                .query(req.q())
                .filters(filters)
                .businessId(req.businessId())
                .build();

        String[] sortParts = parseSort(req.sort());
        SearchResponse<UserDocument> result = userSearch.search(
                criteria, req.pageOrDefault(), req.sizeOrDefault(), sortParts[0], sortParts[1]);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<SearchResponse<RoleDocument>>> searchRoles(@RequestBody SearchRequest req) {
        Map<String, Object> filters = new HashMap<>();
        if (req.enabled() != null) filters.put("enabled", req.enabled());

        SearchCriteria criteria = SearchCriteria.builder()
                .query(req.q())
                .filters(filters)
                .businessId(req.businessId())
                .build();

        String[] sortParts = parseSort(req.sort());
        SearchResponse<RoleDocument> result = roleSearch.search(
                criteria, req.pageOrDefault(), req.sizeOrDefault(), sortParts[0], sortParts[1]);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/third-parties")
    public ResponseEntity<ApiResponse<SearchResponse<ThirdPartyDocument>>> searchThirdParties(@RequestBody SearchRequest req) {
        Map<String, Object> filters = new HashMap<>();
        if (req.enabled() != null) filters.put("enabled", req.enabled());

        SearchCriteria criteria = SearchCriteria.builder()
                .query(req.q())
                .filters(filters)
                .businessId(req.businessId())
                .build();

        String[] sortParts = parseSort(req.sort());
        SearchResponse<ThirdPartyDocument> result = thirdPartySearch.search(
                criteria, req.pageOrDefault(), req.sizeOrDefault(), sortParts[0], sortParts[1]);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/third-parties/{id}")
    public ResponseEntity<ApiResponse<ThirdPartyDocument>> thirdPartyById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(thirdPartySearch.findById(id)));
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
