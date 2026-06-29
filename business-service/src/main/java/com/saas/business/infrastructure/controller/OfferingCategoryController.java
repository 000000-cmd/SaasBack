package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.OfferingCategoryRequest;
import com.saas.business.application.dto.response.OfferingCategoryResponse;
import com.saas.business.application.mapper.OfferingCategoryMapper;
import com.saas.business.domain.model.OfferingCategory;
import com.saas.business.domain.port.in.IOfferingCategoryUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/offering-categories")
@RequiredArgsConstructor
public class OfferingCategoryController {
    private final IOfferingCategoryUseCase useCase;
    private final OfferingCategoryMapper mapper;
    @GetMapping
    public ResponseEntity<ApiResponse<List<OfferingCategoryResponse>>> byBusiness(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBusiness(businessId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OfferingCategoryResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<OfferingCategoryResponse>> create(@Valid @RequestBody OfferingCategoryRequest req) {
        OfferingCategory c = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(c)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OfferingCategoryResponse>> update(@PathVariable UUID id, @Valid @RequestBody OfferingCategoryRequest req) {
        OfferingCategory e = useCase.getById(id); mapper.updateDomain(req, e);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, e))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id); return ResponseEntity.ok(ApiResponse.success(null, "Categoria de oferta eliminada"));
    }
}
