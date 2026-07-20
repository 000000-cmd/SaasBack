package com.saas.finance.infrastructure.controller;

import com.saas.finance.application.dto.request.BalanceEnsureRequest;
import com.saas.finance.application.dto.response.EmployeeBalanceResponse;
import com.saas.finance.domain.port.in.IEmployeeBalanceUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints internos S2S de saldo (sin JWT; {@code /internal/**} permitido).
 * business-service llama {@code ensure} al aprovisionar un empleado para crear
 * su saldo en 0 y proyectarlo a Elasticsearch.
 */
@RestController
@RequestMapping("/internal/balances")
@RequiredArgsConstructor
public class InternalBalanceController {

    private final IEmployeeBalanceUseCase useCase;

    @PostMapping("/ensure")
    public EmployeeBalanceResponse ensure(@Valid @RequestBody BalanceEnsureRequest req) {
        return EmployeeBalanceController.toResponse(useCase.ensure(
                req.employeeId(), req.businessId(), req.branchId(), req.thirdPartyId(), req.userId()));
    }
}
