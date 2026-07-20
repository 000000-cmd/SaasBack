package com.saas.finance.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.finance.application.dto.response.EmployeeBalanceResponse;
import com.saas.finance.domain.model.EmployeeBalance;
import com.saas.finance.domain.port.in.IEmployeeBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Lectura del saldo por cobrar del empleado desde la FUENTE (finance). El APK
 * lee normalmente desde Elasticsearch (mas rapido); estos endpoints son la
 * fuente de verdad / fallback si ES aun no proyectó.
 */
@RestController
@RequestMapping("/balances")
@RequiredArgsConstructor
public class EmployeeBalanceController {

    private final IEmployeeBalanceUseCase useCase;

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeBalanceResponse>> byEmployee(@PathVariable UUID employeeId) {
        return useCase.findByEmployee(employeeId)
                .map(b -> ResponseEntity.ok(ApiResponse.success(toResponse(b))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(null)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<EmployeeBalanceResponse>> byUser(@RequestParam UUID userId) {
        return useCase.findByUser(userId)
                .map(b -> ResponseEntity.ok(ApiResponse.success(toResponse(b))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(null)));
    }

    static EmployeeBalanceResponse toResponse(EmployeeBalance b) {
        return new EmployeeBalanceResponse(
                b.getId(), b.getBusinessId(), b.getBranchId(), b.getEmployeeId(), b.getThirdPartyId(), b.getUserId(),
                b.getAmountAccrued(), b.getAmountPaid(), b.getBalance(), b.getCurrency(), b.getLastCalculatedAt());
    }
}
