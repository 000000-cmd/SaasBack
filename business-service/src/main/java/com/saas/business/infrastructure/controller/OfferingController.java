package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.OfferingRequest;
import com.saas.business.application.dto.response.OfferingResponse;
import com.saas.business.application.mapper.OfferingMapper;
import com.saas.business.domain.model.Offering;
import com.saas.business.domain.port.in.IOfferingUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/offerings")
@RequiredArgsConstructor
public class OfferingController {
    private final IOfferingUseCase useCase;
    private final OfferingMapper mapper;
    @GetMapping
    public ResponseEntity<ApiResponse<List<OfferingResponse>>> byBusiness(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBusiness(businessId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OfferingResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<OfferingResponse>> create(@Valid @RequestBody OfferingRequest req) {
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(useCase.create(mapper.toDomain(req)))));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OfferingResponse>> update(@PathVariable UUID id, @Valid @RequestBody OfferingRequest req) {
        Offering e = useCase.getById(id); mapper.updateDomain(req, e);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, e))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id); return ResponseEntity.ok(ApiResponse.success(null, "Oferta eliminada"));
    }
}
