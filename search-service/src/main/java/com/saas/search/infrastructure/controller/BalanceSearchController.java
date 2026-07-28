package com.saas.search.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.search.application.dto.search.BalanceSearchRequest;
import com.saas.search.domain.document.EmployeeBalanceDocument;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lectura del saldo por cobrar desde Elasticsearch (read model). Es la via que
 * usan el APK y la pantalla de liquidacion: rapida y desacoplada de la BD
 * transaccional.
 *
 * <p>Un solo endpoint; los criterios van en el cuerpo. Ver
 * {@link BalanceSearchRequest}.
 */
@RestController
@RequiredArgsConstructor
public class BalanceSearchController {

    private final ElasticsearchOperations ops;
    private final IndexNames indexNames;

    // La ruta va completa en el metodo: un @PostMapping sin valor bajo un
    // @RequestMapping de clase no llega a registrarse.
    @PostMapping("/balances")
    public ResponseEntity<ApiResponse<List<EmployeeBalanceDocument>>> search(
            @RequestBody BalanceSearchRequest req) {

        Criteria criteria = null;
        criteria = and(criteria, "businessId", req.businessId());
        criteria = and(criteria, "branchId", req.branchId());
        criteria = and(criteria, "userId", req.userId());
        criteria = and(criteria, "employeeId", req.employeeId());

        // Sin ningun criterio la consulta traeria el indice entero: se corta aqui
        // en vez de dejar que un cuerpo vacio se lleve todos los saldos.
        if (criteria == null) return ResponseEntity.ok(ApiResponse.success(List.of()));

        CriteriaQuery query = new CriteriaQuery(criteria)
                .addSort(Sort.by(Sort.Direction.DESC, "balance"));
        query.setMaxResults(req.sizeOrDefault());

        List<EmployeeBalanceDocument> docs = ops
                .search(query, EmployeeBalanceDocument.class, IndexCoordinates.of(indexNames.employeeBalances()))
                .getSearchHits().stream().map(hit -> hit.getContent()).toList();

        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    /** Encadena un filtro solo si trae valor. */
    private Criteria and(Criteria base, String field, String value) {
        if (value == null || value.isBlank()) return base;
        return base == null ? Criteria.where(field).is(value) : base.and(field).is(value);
    }
}
