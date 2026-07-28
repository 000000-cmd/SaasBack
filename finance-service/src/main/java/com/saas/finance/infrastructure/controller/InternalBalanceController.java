package com.saas.finance.infrastructure.controller;

import com.saas.finance.application.dto.event.EmployeeBalanceEventPayload;
import com.saas.finance.application.dto.request.BalanceEnsureRequest;
import com.saas.finance.application.dto.response.EmployeeBalanceResponse;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.finance.domain.port.in.IEmployeeBalanceUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /**
     * Página de saldos para el REINDEX de search-service. Devuelve el mismo
     * payload que se publica al outbox, así el documento en ES queda idéntico
     * venga por evento o por reindex.
     */
    @GetMapping("/all")
    public List<EmployeeBalanceEventPayload> all(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "500") int size) {
        return useCase.findAllPaged(page, size).stream()
                .map(EmployeeBalanceEventPayload::from)
                .toList();
    }

    @GetMapping("/count")
    public Map<String, Long> count() {
        return Map.of("total", useCase.count());
    }

    /** Payload de UN saldo, para el reindex puntual desde la gestion de Elastic. */
    @GetMapping("/one/{id}")
    public EmployeeBalanceEventPayload one(@PathVariable UUID id) {
        return useCase.findById(id)
                .map(EmployeeBalanceEventPayload::from)
                .orElseThrow(() -> new ResourceNotFoundException("Saldo", "id", id));
    }
}
