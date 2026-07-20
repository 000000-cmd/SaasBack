package com.saas.search.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.search.domain.document.EmployeeBalanceDocument;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lectura del saldo por cobrar del empleado desde Elasticsearch (read model).
 * Es la via que usa el APK: rapida y desacoplada de la BD transaccional.
 */
@RestController
@RequestMapping("/balances")
@RequiredArgsConstructor
public class BalanceSearchController {

    /** Techo de la lista por negocio: ningun negocio liquida mas empleados de una vez. */
    private static final int MAX_BALANCES = 500;

    private final ElasticsearchOperations ops;
    private final IndexNames indexNames;

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<ApiResponse<EmployeeBalanceDocument>> byUser(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(findOne("userId", userId)));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeBalanceDocument>> byEmployee(@PathVariable String employeeId) {
        return ResponseEntity.ok(ApiResponse.success(findOne("employeeId", employeeId)));
    }

    /**
     * Saldos de todo el negocio (opcionalmente de una sede). Es la lista que
     * alimenta la pantalla de liquidacion del dueno: leerla de ES evita el join
     * saldo x empleado contra la BD transaccional en cada carga.
     */
    @GetMapping("/by-business/{businessId}")
    public ResponseEntity<ApiResponse<List<EmployeeBalanceDocument>>> byBusiness(
            @PathVariable String businessId,
            @RequestParam(required = false) String branchId) {

        Criteria criteria = Criteria.where("businessId").is(businessId);
        if (branchId != null && !branchId.isBlank()) criteria = criteria.and("branchId").is(branchId);

        CriteriaQuery query = new CriteriaQuery(criteria)
                .addSort(Sort.by(Sort.Direction.DESC, "balance"));
        query.setMaxResults(MAX_BALANCES);

        List<EmployeeBalanceDocument> docs = ops
                .search(query, EmployeeBalanceDocument.class, IndexCoordinates.of(indexNames.employeeBalances()))
                .getSearchHits().stream().map(hit -> hit.getContent()).toList();
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    /** Primer documento cuyo {@code field} == {@code value}, o null. */
    private EmployeeBalanceDocument findOne(String field, String value) {
        CriteriaQuery query = new CriteriaQuery(Criteria.where(field).is(value));
        SearchHits<EmployeeBalanceDocument> hits = ops.search(
                query, EmployeeBalanceDocument.class, IndexCoordinates.of(indexNames.employeeBalances()));
        return hits.getTotalHits() > 0 ? hits.getSearchHit(0).getContent() : null;
    }
}
