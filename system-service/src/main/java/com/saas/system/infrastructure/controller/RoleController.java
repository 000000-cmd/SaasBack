package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.request.RoleRequest;
import com.saas.system.application.dto.response.RoleResponse;
import com.saas.system.application.mapper.RoleMapper;
import com.saas.system.domain.model.Role;
import com.saas.system.domain.port.in.IRoleUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de Roles.
 */
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleUseCase roleUseCase;
    private final RoleMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleRequest request) {
        Role domain = mapper.toDomain(request);
        Role created = roleUseCase.create(domain);
        RoleResponse response = mapper.toResponse(created);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAll() {
        List<Role> roles = roleUseCase.getAll();
        List<RoleResponse> response = mapper.toResponseList(roles);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<RoleResponse>> getByCode(@PathVariable String code) {
        Role role = roleUseCase.getByCode(code);
        RoleResponse response = mapper.toResponse(role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getById(@PathVariable String id) {
        Role role = roleUseCase.getById(id);
        RoleResponse response = mapper.toResponse(role);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody RoleRequest request) {
        Role domain = mapper.toDomain(request);
        Role updated = roleUseCase.update(id, domain);
        RoleResponse response = mapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response, "Rol actualizado exitosamente"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(
            @PathVariable String id,
            @RequestParam boolean enabled) {
        roleUseCase.toggleEnabled(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(null,
                enabled ? "Rol habilitado" : "Rol deshabilitado"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        roleUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Rol eliminado exitosamente"));
    }
}
