package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.request.PermissionRequest;
import com.saas.system.application.dto.response.PermissionResponse;
import com.saas.system.application.mapper.PermissionMapper;
import com.saas.system.domain.model.Permission;
import com.saas.common.security.IUserPrincipal;
import com.saas.system.domain.port.in.IPermissionUseCase;
import com.saas.system.domain.port.in.IRolePermissionUseCase;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final IPermissionUseCase useCase;
    private final IRolePermissionUseCase rolePermissionUseCase;
    private final PermissionMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(useCase.getAll().stream().map(mapper::toResponse).toList()));
    }

    /** Codigos de permisos efectivos del usuario autenticado (segun sus roles del JWT). */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Set<String>>> myPermissions() {
        return ResponseEntity.ok(ApiResponse.success(
                rolePermissionUseCase.getPermissionCodesByRoleCodes(currentRoles())));
    }

    private Set<String> currentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof IUserPrincipal p && p.getRoles() != null) {
            return p.getRoles();
        }
        return Set.of();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<PermissionResponse>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getByCode(code))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PermissionResponse>> create(@Valid @RequestBody PermissionRequest req) {
        Permission created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PermissionResponse>> update(@PathVariable UUID id, @Valid @RequestBody PermissionRequest req) {
        Permission existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Permiso deshabilitado"));
    }
}
