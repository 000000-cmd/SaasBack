package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.common.security.IUserPrincipal;
import com.saas.system.application.dto.request.AssignIdsRequest;
import com.saas.system.application.dto.request.MenuRequest;
import com.saas.system.application.dto.response.MenuResponse;
import com.saas.system.application.mapper.MenuMapper;
import com.saas.system.domain.model.Menu;
import com.saas.system.domain.port.in.IMenuRoleUseCase;
import com.saas.system.domain.port.in.IMenuUseCase;
import com.saas.system.domain.port.in.IRoleUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

    private final IMenuUseCase menuUseCase;
    private final IMenuRoleUseCase menuRoleUseCase;
    private final IRoleUseCase roleUseCase;
    private final MenuMapper mapper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> listFlat() {
        return ResponseEntity.ok(ApiResponse.success(menuUseCase.getAll().stream().map(mapper::toResponse).toList()));
    }

    /** Arbol completo (admin). */
    @GetMapping("/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> tree() {
        return ResponseEntity.ok(ApiResponse.success(buildTree(menuUseCase.getAll())));
    }

    /** Arbol del usuario actual: solo menus visibles segun sus roles. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> menusForCurrentUser(
            @AuthenticationPrincipal IUserPrincipal principal) {
        Set<UUID> roleIds = roleUseCase.getAll().stream()
                .filter(r -> principal.getRoles().contains(r.getCode()))
                .map(com.saas.system.domain.model.Role::getId)
                .collect(Collectors.toSet());
        List<Menu> visible = menuUseCase.getMenusForRoles(roleIds);
        return ResponseEntity.ok(ApiResponse.success(buildTree(visible)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(menuUseCase.getById(id))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuResponse>> create(@Valid @RequestBody MenuRequest req) {
        Menu created = menuUseCase.create(toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MenuResponse>> update(@PathVariable UUID id, @Valid @RequestBody MenuRequest req) {
        Menu existing = menuUseCase.getById(id);
        mapper.updateDomain(req, existing);
        existing.setParentId(req.parentId()); // permitir mover a root
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(menuUseCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        menuUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Menu deshabilitado"));
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Set<UUID>>> getRoles(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(menuRoleUseCase.getRoleIdsForMenu(id)));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setRoles(@PathVariable UUID id, @Valid @RequestBody AssignIdsRequest req) {
        menuRoleUseCase.replaceRolesForMenu(id, req.ids());
        return ResponseEntity.ok(ApiResponse.success(null, "Roles asignados al menu"));
    }

    private Menu toDomain(MenuRequest req) {
        Menu m = mapper.toDomain(req);
        m.setParentId(req.parentId());
        return m;
    }

    /** Construye el arbol jerarquico a partir de una lista plana. */
    private List<MenuResponse> buildTree(List<Menu> flat) {
        Map<UUID, MenuResponse> base = new HashMap<>();
        for (Menu m : flat) base.put(m.getId(), mapper.toResponse(m));

        List<MenuResponse> roots = new ArrayList<>();
        Map<UUID, List<MenuResponse>> childrenIdx = new HashMap<>();

        for (Menu m : flat) {
            MenuResponse r = base.get(m.getId());
            if (m.getParentId() == null) {
                roots.add(r);
            } else {
                childrenIdx.computeIfAbsent(m.getParentId(), k -> new ArrayList<>()).add(r);
            }
        }

        // Reconstruir cada nodo aniadiendo children (records son inmutables -> rebuild)
        return roots.stream().map(r -> withChildren(r, childrenIdx)).toList();
    }

    private MenuResponse withChildren(MenuResponse r, Map<UUID, List<MenuResponse>> idx) {
        List<MenuResponse> kids = idx.getOrDefault(r.id(), List.of()).stream()
                .map(c -> withChildren(c, idx))
                .toList();
        return new MenuResponse(r.id(), r.code(), r.name(), r.icon(), r.route(),
                r.parentId(), r.displayOrder(), r.enabled(), r.visible(),
                r.createdDate(), r.auditDate(), kids);
    }
}
