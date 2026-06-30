package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.EmployeeRequest;
import com.saas.business.application.dto.response.EmployeeDetailResponse;
import com.saas.business.application.dto.response.EmployeeResponse;
import com.saas.business.application.mapper.EmployeeMapper;
import com.saas.business.domain.model.Employee;
import com.saas.business.domain.port.in.IEmployeeUseCase;
import com.saas.business.infrastructure.client.ThirdPartyClient;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {
    private final IEmployeeUseCase useCase;
    private final EmployeeMapper mapper;
    private final ThirdPartyClient thirdPartyClient;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> byBranch(@RequestParam UUID branchId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBranch(branchId))));
    }

    /** Empleados de una sede con el nombre de la persona ya resuelto (via Feign batch). */
    @GetMapping("/detailed")
    public ResponseEntity<ApiResponse<List<EmployeeDetailResponse>>> detailedByBranch(@RequestParam UUID branchId) {
        List<Employee> emps = useCase.findByBranch(branchId);
        Set<UUID> ids = emps.stream().map(Employee::getThirdPartyId).collect(Collectors.toSet());
        Map<String, String> names = Map.of();
        if (!ids.isEmpty()) {
            try { names = thirdPartyClient.personNames(ids); }
            catch (Exception ex) { log.debug("No se pudieron resolver nombres de personas: {}", ex.getMessage()); }
        }
        final Map<String, String> resolved = names;
        List<EmployeeDetailResponse> out = emps.stream()
                .map(e -> new EmployeeDetailResponse(
                        e.getId(), e.getThirdPartyId(),
                        resolved.getOrDefault(e.getThirdPartyId().toString(), "—"),
                        e.getBranchId(), e.getPositionId(), e.getEmployeeCode(),
                        e.getHireDate(), e.getTerminationDate(), e.getStatusId(), e.getEnabled()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(out));
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
