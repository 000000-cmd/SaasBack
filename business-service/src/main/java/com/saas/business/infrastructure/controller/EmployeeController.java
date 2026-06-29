package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.EmployeeRequest;
import com.saas.business.application.dto.response.EmployeeResponse;
import com.saas.business.application.mapper.EmployeeMapper;
import com.saas.business.domain.model.Employee;
import com.saas.business.domain.port.in.IEmployeeUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final IEmployeeUseCase useCase;
    private final EmployeeMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> byBranch(@RequestParam UUID branchId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBranch(branchId))));
    }
    @GetMapping("/third-party/{thirdPartyId}")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> byThirdParty(@PathVariable UUID thirdPartyId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByThirdParty(thirdPartyId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest req) {
        Employee created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(@PathVariable UUID id, @Valid @RequestBody EmployeeRequest req) {
        Employee existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Empleado deshabilitado"));
    }
}
