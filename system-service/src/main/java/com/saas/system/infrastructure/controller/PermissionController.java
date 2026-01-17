package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.request.PermissionRequest;
import com.saas.system.application.dto.response.PermissionResponse;
import com.saas.system.application.mapper.PermissionMapper;
import com.saas.system.domain.model.Permission;
import com.saas.system.domain.port.in.IPermissionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de Permisos.
 */
@RestController
@RequestMapping("/api/system/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final IPermissionUseCase permissionUseCase;
    private final PermissionMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<PermissionResponse>> create(@Valid @RequestBody PermissionRequest request) {
        Permission domain = mapper.toDomain(request);
        Permission created = permissionUseCase.create(domain);
        PermissionResponse response = mapper.toResponse(created);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAll() {
        List<Permission> permissions = permissionUseCase.getAll();
        List<PermissionResponse> response = mapper.toResponseList(permissions);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<PermissionResponse>> getByCode(@PathVariable String code) {
        Permission permission = permissionUseCase.getByCode(code);
        PermissionResponse response = mapper.toResponse(permission);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<PermissionResponse>> getById(@PathVariable String id) {
        Permission permission = permissionUseCase.getById(id);
        PermissionResponse response = mapper.toResponse(permission);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody PermissionRequest request) {
        Permission domain = mapper.toDomain(request);
        Permission updated = permissionUseCase.update(id, domain);
        PermissionResponse response = mapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response, "Permiso actualizado exitosamente"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(
            @PathVariable String id,
            @RequestParam boolean enabled) {
        permissionUseCase.toggleEnabled(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(null,
                enabled ? "Permiso habilitado" : "Permiso deshabilitado"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        permissionUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Permiso eliminado exitosamente"));
    }
}