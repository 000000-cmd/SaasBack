package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.EmployeeShiftAssignmentRequest;
import com.saas.business.application.dto.response.EmployeeShiftAssignmentResponse;
import com.saas.business.application.mapper.EmployeeShiftAssignmentMapper;
import com.saas.business.domain.model.EmployeeShiftAssignment;
import com.saas.business.domain.port.in.IEmployeeShiftAssignmentUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employee-shift-assignments")
@RequiredArgsConstructor
public class EmployeeShiftAssignmentController {
    private final IEmployeeShiftAssignmentUseCase useCase;
    private final EmployeeShiftAssignmentMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeShiftAssignmentResponse>>> history(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByEmployee(employeeId))));
    }
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<EmployeeShiftAssignmentResponse>>> current(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findCurrentByEmployee(employeeId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeShiftAssignmentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeShiftAssignmentResponse>> create(@Valid @RequestBody EmployeeShiftAssignmentRequest req) {
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(useCase.create(mapper.toDomain(req)))));
    }
    @PutMapping("/{id}/supersede")
    public ResponseEntity<ApiResponse<EmployeeShiftAssignmentResponse>> supersede(@PathVariable UUID id, @Valid @RequestBody EmployeeShiftAssignmentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.supersede(id, mapper.toDomain(req)))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id); return ResponseEntity.ok(ApiResponse.success(null, "Asignacion eliminada"));
    }
}
