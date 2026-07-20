package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.SpecialtyRequest;
import com.saas.business.application.dto.response.SpecialtyResponse;
import com.saas.business.application.mapper.SpecialtyMapper;
import com.saas.business.domain.model.Specialty;
import com.saas.business.domain.port.in.ISpecialtyUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/specialties")
@RequiredArgsConstructor
public class SpecialtyController {
    private final ISpecialtyUseCase useCase;
    private final SpecialtyMapper mapper;
    @GetMapping
    public ResponseEntity<ApiResponse<List<SpecialtyResponse>>> byBusiness(@RequestParam UUID businessId) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.findByBusiness(businessId))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SpecialtyResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<SpecialtyResponse>> create(@Valid @RequestBody SpecialtyRequest req) {
        Specialty c = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(c)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SpecialtyResponse>> update(@PathVariable UUID id, @Valid @RequestBody SpecialtyRequest req) {
        Specialty e = useCase.getById(id); mapper.updateDomain(req, e);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, e))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id); return ResponseEntity.ok(ApiResponse.success(null, "Especialidad eliminada"));
    }
}
