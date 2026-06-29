package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BusinessOwnerRequest;
import com.saas.business.application.dto.response.BusinessOwnerResponse;
import com.saas.business.application.mapper.BusinessOwnerMapper;
import com.saas.business.domain.model.BusinessOwner;
import com.saas.business.domain.port.in.IBusinessOwnerUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/business-owners")
@RequiredArgsConstructor
public class BusinessOwnerController {
    private final IBusinessOwnerUseCase useCase;
    private final BusinessOwnerMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusinessOwnerResponse>>> byBusiness(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBusiness(businessId))));
    }
    @GetMapping("/third-party/{thirdPartyId}")
    public ResponseEntity<ApiResponse<List<BusinessOwnerResponse>>> byThirdParty(@PathVariable UUID thirdPartyId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByThirdParty(thirdPartyId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessOwnerResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<BusinessOwnerResponse>> create(@Valid @RequestBody BusinessOwnerRequest req) {
        BusinessOwner created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessOwnerResponse>> update(@PathVariable UUID id, @Valid @RequestBody BusinessOwnerRequest req) {
        BusinessOwner existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Propietario removido"));
    }
}
