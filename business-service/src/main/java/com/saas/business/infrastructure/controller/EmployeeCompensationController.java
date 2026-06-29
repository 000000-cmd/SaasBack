package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.EmployeeCompensationRequest;
import com.saas.business.application.dto.response.EmployeeCompensationResponse;
import com.saas.business.application.mapper.EmployeeCompensationMapper;
import com.saas.business.domain.model.EmployeeCompensation;
import com.saas.business.domain.port.in.IEmployeeCompensationUseCase;
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
    private final EmployeeCompensationMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeCompensationResponse>>> history(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByEmployee(employeeId))));
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
