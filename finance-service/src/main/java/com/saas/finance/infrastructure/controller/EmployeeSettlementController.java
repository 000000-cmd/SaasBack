package com.saas.finance.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.finance.application.dto.request.SettlementRequest;
import com.saas.finance.application.dto.response.EmployeeSettlementResponse;
import com.saas.finance.application.mapper.EmployeeSettlementMapper;
import com.saas.finance.domain.port.in.IEmployeeSettlementUseCase;
import com.saas.finance.domain.port.in.IPayrollUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Liquidacion: abona al saldo del empleado el total de sus servicios aprobados.
 * El historial es su extracto (abonos y pagos de nomina en una sola lista).
 */
@RestController
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class EmployeeSettlementController {

    private final IEmployeeSettlementUseCase useCase;
    private final IPayrollUseCase payroll;
    private final EmployeeSettlementMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeSettlementResponse>> settle(@Valid @RequestBody SettlementRequest req) {
        return ResponseEntity.ok(ApiResponse.created(
                mapper.toResponse(useCase.settle(req.employeeId(), req.note()))));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<EmployeeSettlementResponse>>> byEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.historyByEmployee(employeeId))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeSettlementResponse>>> byBusiness(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.historyByBusiness(businessId))));
    }

    /** Un movimiento suelto: lo carga el comprobante desde su propia ruta. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeSettlementResponse>> byId(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.byId(id))));
    }

    /** Detalle de una corrida: un movimiento por empleado pagado. */
    @GetMapping("/run/{payrollRunId}")
    public ResponseEntity<ApiResponse<List<EmployeeSettlementResponse>>> byRun(@PathVariable UUID payrollRunId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.byPayrollRun(payrollRunId))));
    }

    /**
     * El colaborador acusa recibo de un pago en efectivo, desde el móvil.
     *
     * <p>Va aquí y no en /payroll porque es una acción sobre SU movimiento, no
     * sobre la corrida del dueño. El servicio comprueba que el movimiento sea
     * suyo antes de sellarlo.</p>
     */
    @PutMapping("/{id}/confirm-cash")
    public ResponseEntity<ApiResponse<EmployeeSettlementResponse>> confirmCash(
            @PathVariable UUID id, @RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponse(payroll.confirmCash(id, employeeId))));
    }
}
