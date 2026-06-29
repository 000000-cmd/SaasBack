package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BusinessRequest;
import com.saas.business.application.dto.response.BusinessResponse;
import com.saas.business.application.mapper.BusinessMapper;
import com.saas.business.domain.model.Business;
import com.saas.business.domain.port.in.IBusinessUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final IBusinessUseCase useCase;
    private final BusinessMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusinessResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.getAll())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusinessResponse>> create(@Valid @RequestBody BusinessRequest req) {
        Business created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessResponse>> update(@PathVariable UUID id,
                                                               @Valid @RequestBody BusinessRequest req) {
        Business existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Empresa deshabilitada"));
    }
}
