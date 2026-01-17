package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.request.RoleMenuRequest;
import com.saas.system.application.dto.response.RoleMenuResponse;
import com.saas.system.application.mapper.RoleMenuMapper;
import com.saas.system.domain.model.RoleMenu;
import com.saas.system.domain.port.in.IRoleMenuUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de asignaciones de Menús a Roles.
 */
@RestController
@RequestMapping("/api/system/role-menus")
@RequiredArgsConstructor
public class RoleMenuController {

    private final IRoleMenuUseCase roleMenuUseCase;
    private final RoleMenuMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<RoleMenuResponse>> assignMenu(@Valid @RequestBody RoleMenuRequest request) {
        RoleMenu created = roleMenuUseCase.assignMenuToRole(request.getRoleCode(), request.getMenuCode());
        RoleMenuResponse response = mapper.toResponse(created);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Menú asignado al rol exitosamente"));
    }

    @GetMapping("/role/{roleCode}")
    public ResponseEntity<ApiResponse<List<RoleMenuResponse>>> getMenusByRole(@PathVariable String roleCode) {
        List<RoleMenu> roleMenus = roleMenuUseCase.getMenusByRoleCode(roleCode);
        List<RoleMenuResponse> response = mapper.toResponseList(roleMenus);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeAssignment(@PathVariable String id) {
        roleMenuUseCase.removeAssignment(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Asignación eliminada exitosamente"));
    }

    @DeleteMapping("/role/{roleCode}/menu/{menuCode}")
    public ResponseEntity<ApiResponse<Void>> removeAssignmentByRoleAndMenu(
            @PathVariable String roleCode,
            @PathVariable String menuCode) {
        roleMenuUseCase.removeAssignment(roleCode, menuCode);
        return ResponseEntity.ok(ApiResponse.success(null, "Asignación eliminada exitosamente"));
    }
}