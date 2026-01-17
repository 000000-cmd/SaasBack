package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.request.RoleMenuPermissionRequest;
import com.saas.system.application.dto.response.RoleMenuPermissionResponse;
import com.saas.system.application.mapper.RoleMenuPermissionMapper;
import com.saas.system.domain.model.RoleMenuPermission;
import com.saas.system.domain.port.in.IRoleMenuPermissionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de Permisos por RoleMenu.
 */
@RestController
@RequestMapping("/api/system/role-menu-permissions")
@RequiredArgsConstructor
public class RoleMenuPermissionController {

    private final IRoleMenuPermissionUseCase roleMenuPermissionUseCase;
    private final RoleMenuPermissionMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<RoleMenuPermissionResponse>> assignPermission(
            @Valid @RequestBody RoleMenuPermissionRequest request) {
        RoleMenuPermission created = roleMenuPermissionUseCase.assignPermission(
                request.getRoleMenuId(), request.getPermissionCode());
        RoleMenuPermissionResponse response = mapper.toResponse(created);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Permiso asignado exitosamente"));
    }

    @GetMapping("/role-menu/{roleMenuId}")
    public ResponseEntity<ApiResponse<List<RoleMenuPermissionResponse>>> getPermissionsByRoleMenu(
            @PathVariable String roleMenuId) {
        List<RoleMenuPermission> permissions = roleMenuPermissionUseCase.getPermissionsByRoleMenuId(roleMenuId);
        List<RoleMenuPermissionResponse> response = mapper.toResponseList(permissions);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removePermission(@PathVariable String id) {
        roleMenuPermissionUseCase.removePermission(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Permiso eliminado exitosamente"));
    }

    @DeleteMapping("/role-menu/{roleMenuId}")
    public ResponseEntity<ApiResponse<Void>> removeAllPermissionsFromRoleMenu(@PathVariable String roleMenuId) {
        roleMenuPermissionUseCase.removeAllPermissionsFromRoleMenu(roleMenuId);
        return ResponseEntity.ok(ApiResponse.success(null, "Todos los permisos eliminados exitosamente"));
    }
}
