package com.saas.finance.infrastructure.controller;

import com.saas.finance.application.dto.request.BusinessCompensationRequest;
import com.saas.finance.application.dto.response.BusinessCompensationResponse;
import com.saas.finance.application.mapper.BusinessCompensationMapper;
import com.saas.finance.domain.port.in.IBusinessCompensationUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/business-compensations")
@RequiredArgsConstructor
public class BusinessCompensationController {
    private final IBusinessCompensationUseCase useCase;
    private final BusinessCompensationMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusinessCompensationResponse>>> history(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBusiness(businessId))));
    }
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<BusinessCompensationResponse>> current(@RequestParam UUID businessId) {
        return useCase.findCurrentByBusiness(businessId)
                .map(c -> ResponseEntity.ok(ApiResponse.success(mapper.toResponse(c))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("Sin compensacion vigente", 404)));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessCompensationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<BusinessCompensationResponse>> create(@Valid @RequestBody BusinessCompensationRequest req) {
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(useCase.create(mapper.toDomain(req)))));
    }
    @PutMapping("/{id}/supersede")
    public ResponseEntity<ApiResponse<BusinessCompensationResponse>> supersede(@PathVariable UUID id, @Valid @RequestBody BusinessCompensationRequest req) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.supersede(id, mapper.toDomain(req)))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id); return ResponseEntity.ok(ApiResponse.success(null, "Compensacion eliminada"));
    }
}
