package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BusinessLandingRequest;
import com.saas.business.application.dto.response.BusinessLandingResponse;
import com.saas.business.application.mapper.BusinessLandingMapper;
import com.saas.business.domain.port.in.IBusinessLandingUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Edición de la landing del negocio (autenticado; el dueño la maneja desde
 * "Mi página"). El PUT es upsert: la landing existe desde el primer guardado.
 */
@RestController
@RequestMapping("/landing")
@RequiredArgsConstructor
public class LandingController {

    private final IBusinessLandingUseCase useCase;
    private final BusinessLandingMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<BusinessLandingResponse>> byBusiness(@RequestParam UUID businessId) {
        return useCase.findByBusiness(businessId)
                .map(l -> ResponseEntity.ok(ApiResponse.success(mapper.toResponse(l))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("Sin landing configurada", 404)));
    }

    @PutMapping("/{businessId}")
    public ResponseEntity<ApiResponse<BusinessLandingResponse>> upsert(@PathVariable UUID businessId,
                                                                       @Valid @RequestBody BusinessLandingRequest request) {
        BusinessLandingResponse saved = mapper.toResponse(useCase.upsert(businessId, mapper.toDomain(request)));
        return ResponseEntity.ok(ApiResponse.success(saved, "Landing guardada"));
    }
}
