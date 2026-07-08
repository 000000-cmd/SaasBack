package com.saas.finance.infrastructure.controller;

import com.saas.finance.application.dto.request.BranchCompensationRequest;
import com.saas.finance.application.dto.response.BranchCompensationResponse;
import com.saas.finance.application.mapper.BranchCompensationMapper;
import com.saas.finance.domain.port.in.IBranchCompensationUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/branch-compensations")
@RequiredArgsConstructor
public class BranchCompensationController {
    private final IBranchCompensationUseCase useCase;
    private final BranchCompensationMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchCompensationResponse>>> history(@RequestParam UUID branchId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBranch(branchId))));
    }
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<BranchCompensationResponse>> current(@RequestParam UUID branchId) {
        return useCase.findCurrentByBranch(branchId)
                .map(c -> ResponseEntity.ok(ApiResponse.success(mapper.toResponse(c))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("Sin compensacion vigente", 404)));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchCompensationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<BranchCompensationResponse>> create(@Valid @RequestBody BranchCompensationRequest req) {
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(useCase.create(mapper.toDomain(req)))));
    }
    @PutMapping("/{id}/supersede")
    public ResponseEntity<ApiResponse<BranchCompensationResponse>> supersede(@PathVariable UUID id, @Valid @RequestBody BranchCompensationRequest req) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.supersede(id, mapper.toDomain(req)))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id); return ResponseEntity.ok(ApiResponse.success(null, "Compensacion eliminada"));
    }
}
