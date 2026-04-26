package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.request.AssignIdsRequest;
import com.saas.system.application.dto.request.RoleRequest;
import com.saas.system.application.dto.response.PermissionResponse;
import com.saas.system.application.dto.response.RoleResponse;
import com.saas.system.application.mapper.PermissionMapper;
import com.saas.system.application.mapper.RoleMapper;
import com.saas.system.domain.model.Role;
import com.saas.system.domain.port.in.IRolePermissionUseCase;
import com.saas.system.domain.port.in.IRoleUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleUseCase roleUseCase;
    private final IRolePermissionUseCase rolePermUseCase;
    private final RoleMapper mapper;
    private final PermissionMapper permMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(roleUseCase.getAll().stream().map(mapper::toResponse).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(roleUseCase.getById(id))));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<RoleResponse>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(roleUseCase.getByCode(code))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody RoleRequest req) {
        Role created = roleUseCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleResponse>> update(@PathVariable UUID id, @Valid @RequestBody RoleRequest req) {
        Role existing = roleUseCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(roleUseCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        roleUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Rol deshabilitado"));
    }

    // --- Sub-recurso: permisos del rol ---

    @GetMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                rolePermUseCase.getPermissionsByRoleId(id).stream().map(permMapper::toResponse).toList()));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setPermissions(@PathVariable UUID id,
                                                              @Valid @RequestBody AssignIdsRequest req) {
        rolePermUseCase.replacePermissionsForRole(id, req.ids());
        return ResponseEntity.ok(ApiResponse.success(null, "Permisos asignados"));
    }
}
