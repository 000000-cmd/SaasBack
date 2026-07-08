package com.saas.finance.infrastructure.controller;

import com.saas.finance.application.dto.request.EmployeeCompensationRequest;
import com.saas.finance.application.dto.response.EffectiveCompensationResponse;
import com.saas.finance.application.dto.response.EmployeeCompensationResponse;
import com.saas.finance.application.mapper.EmployeeCompensationMapper;
import com.saas.finance.domain.model.EmployeeCompensation;
import com.saas.finance.domain.port.in.ICompensationResolverUseCase;
import com.saas.finance.domain.port.in.IEmployeeCompensationUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employee-compensations")
@RequiredArgsConstructor
public class EmployeeCompensationController {
    private final IEmployeeCompensationUseCase useCase;
    private final ICompensationResolverUseCase resolver;
    private final EmployeeCompensationMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeCompensationResponse>>> history(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByEmployee(employeeId))));
    }

    /**
     * Compensacion EFECTIVA resolviendo la jerarquia empleado -> sede -> negocio.
     * Devuelve la vigente del primer nivel que la tenga configurada, indicando el origen.
     *
     * <p>{@code branchId} y {@code businessId} son opcionales: finance no posee
     * esas entidades (viven en business), asi que el caller aporta la cadena.
     * Si no se envian, la escalada se limita a los niveles con id presente.</p>
     */
    @GetMapping("/effective")
    public ResponseEntity<ApiResponse<EffectiveCompensationResponse>> effective(
            @RequestParam UUID employeeId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID businessId) {
        return resolver.resolveForEmployee(employeeId, branchId, businessId)
                .map(c -> ResponseEntity.ok(ApiResponse.success(mapper.toEffectiveResponse(c))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("Sin compensacion configurada en ningun nivel", 404)));
    }
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<EmployeeCompensationResponse>> current(@RequestParam UUID employeeId) {
        return useCase.findCurrentByEmployee(employeeId)
                .map(c -> ResponseEntity.ok(ApiResponse.success(mapper.toResponse(c))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("Sin compensacion vigente", 404)));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeCompensationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeCompensationResponse>> create(@Valid @RequestBody EmployeeCompensationRequest req) {
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(useCase.create(mapper.toDomain(req)))));
    }
    @PutMapping("/{id}/supersede")
    public ResponseEntity<ApiResponse<EmployeeCompensationResponse>> supersede(@PathVariable UUID id, @Valid @RequestBody EmployeeCompensationRequest req) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.supersede(id, mapper.toDomain(req)))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id); return ResponseEntity.ok(ApiResponse.success(null, "Compensacion eliminada"));
    }
}
