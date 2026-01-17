package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.request.ConstantRequest;
import com.saas.system.application.dto.response.ConstantResponse;
import com.saas.system.application.mapper.ConstantMapper;
import com.saas.system.domain.model.Constant;
import com.saas.system.domain.port.in.IConstantUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de Constantes.
 */
@RestController
@RequestMapping("/api/system/constants")
@RequiredArgsConstructor
public class ConstantController {

    private final IConstantUseCase constantUseCase;
    private final ConstantMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<ConstantResponse>> create(@Valid @RequestBody ConstantRequest request) {
        Constant domain = mapper.toDomain(request);
        Constant created = constantUseCase.create(domain);
        ConstantResponse response = mapper.toResponse(created);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConstantResponse>>> getAll() {
        List<Constant> constants = constantUseCase.getAll();
        List<ConstantResponse> response = mapper.toResponseList(constants);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<ConstantResponse>>> getByCategory(@PathVariable String category) {
        List<Constant> constants = constantUseCase.getByCategory(category);
        List<ConstantResponse> response = mapper.toResponseList(constants);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<ConstantResponse>> getByCode(@PathVariable String code) {
        Constant constant = constantUseCase.getByCode(code);
        ConstantResponse response = mapper.toResponse(constant);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<ConstantResponse>> getById(@PathVariable String id) {
        Constant constant = constantUseCase.getById(id);
        ConstantResponse response = mapper.toResponse(constant);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ConstantResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody ConstantRequest request) {
        Constant domain = mapper.toDomain(request);
        Constant updated = constantUseCase.update(id, domain);
        ConstantResponse response = mapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response, "Constante actualizada exitosamente"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(
            @PathVariable String id,
            @RequestParam boolean enabled) {
        constantUseCase.toggleEnabled(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(null,
                enabled ? "Constante habilitada" : "Constante deshabilitada"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        constantUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Constante eliminada exitosamente"));
    }
}
