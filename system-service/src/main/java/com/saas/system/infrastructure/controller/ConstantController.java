package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.request.ConstantRequest;
import com.saas.system.application.dto.response.ConstantResponse;
import com.saas.system.application.mapper.ConstantMapper;
import com.saas.system.domain.model.Constant;
import com.saas.system.domain.port.in.IConstantUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/constants")
@RequiredArgsConstructor
public class ConstantController {

    private final IConstantUseCase useCase;
    private final ConstantMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConstantResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(useCase.getAll().stream().map(mapper::toResponse).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConstantResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<ConstantResponse>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getByCode(code))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConstantResponse>> create(@Valid @RequestBody ConstantRequest req) {
        Constant created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConstantResponse>> update(@PathVariable UUID id, @Valid @RequestBody ConstantRequest req) {
        Constant existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Constante deshabilitada"));
    }
}
