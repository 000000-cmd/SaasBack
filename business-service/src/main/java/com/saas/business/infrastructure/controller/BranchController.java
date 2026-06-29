package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BranchRequest;
import com.saas.business.application.dto.response.BranchResponse;
import com.saas.business.application.mapper.BranchMapper;
import com.saas.business.domain.model.Branch;
import com.saas.business.domain.port.in.IBranchUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchController {
    private final IBranchUseCase useCase;
    private final BranchMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchResponse>>> byBusiness(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBusiness(businessId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<BranchResponse>> create(@Valid @RequestBody BranchRequest req) {
        Branch created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> update(@PathVariable UUID id, @Valid @RequestBody BranchRequest req) {
        Branch existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Sede deshabilitada"));
    }
}
