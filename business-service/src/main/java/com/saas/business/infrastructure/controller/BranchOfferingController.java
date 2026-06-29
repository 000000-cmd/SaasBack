package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BranchOfferingRequest;
import com.saas.business.application.dto.response.BranchOfferingResponse;
import com.saas.business.application.mapper.BranchOfferingMapper;
import com.saas.business.domain.model.BranchOffering;
import com.saas.business.domain.port.in.IBranchOfferingUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/branch-offerings")
@RequiredArgsConstructor
public class BranchOfferingController {
    private final IBranchOfferingUseCase useCase;
    private final BranchOfferingMapper mapper;
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchOfferingResponse>>> byBranch(@RequestParam UUID branchId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBranch(branchId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchOfferingResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<BranchOfferingResponse>> create(@Valid @RequestBody BranchOfferingRequest req) {
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(useCase.create(mapper.toDomain(req)))));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchOfferingResponse>> update(@PathVariable UUID id, @Valid @RequestBody BranchOfferingRequest req) {
        BranchOffering e = useCase.getById(id); mapper.updateDomain(req, e);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, e))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id); return ResponseEntity.ok(ApiResponse.success(null, "Oferta de sede eliminado"));
    }
}
