package com.saas.finance.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.finance.application.dto.request.SettlementRequest;
import com.saas.finance.application.dto.response.EmployeeSettlementResponse;
import com.saas.finance.application.mapper.EmployeeSettlementMapper;
import com.saas.finance.domain.port.in.IEmployeeSettlementUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Liquidacion de comisiones. Confirmar es irreversible: registra el movimiento
 * y baja el saldo por cobrar del empleado. El historial es la auditoria de
 * tesoreria que consulta el dueno.
 */
@RestController
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class EmployeeSettlementController {

    private final IEmployeeSettlementUseCase useCase;
    private final EmployeeSettlementMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeSettlementResponse>> settle(@Valid @RequestBody SettlementRequest req) {
        return ResponseEntity.ok(ApiResponse.created(
                mapper.toResponse(useCase.settle(req.employeeId(), req.amount(), req.note()))));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<EmployeeSettlementResponse>>> byEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.historyByEmployee(employeeId))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeSettlementResponse>>> byBusiness(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.historyByBusiness(businessId))));
    }
}
