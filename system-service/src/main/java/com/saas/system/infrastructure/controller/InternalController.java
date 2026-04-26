package com.saas.system.infrastructure.controller;

import com.saas.system.domain.model.Role;
import com.saas.system.domain.port.in.IRolePermissionUseCase;
import com.saas.system.domain.port.in.IRoleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Endpoints S2S (service-to-service) consumidos por auth-service via Feign.
 * Sin {@code ApiResponse} envolvente, sin auth -- la red interna los protege.
 * En la SecurityConfig {@code /internal/**} esta permitAll.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final IRoleUseCase roleUseCase;
    private final IRolePermissionUseCase rolePermUseCase;

    @PostMapping("/roles/codes")
    public Map<UUID, String> resolveRoleCodes(@RequestBody Set<UUID> roleIds) {
        Map<UUID, String> result = new HashMap<>();
        for (Role r : roleUseCase.findByIds(roleIds)) {
            result.put(r.getId(), r.getCode());
        }
        return result;
    }

    @GetMapping("/roles/{roleId}/permissions/codes")
    public Set<String> getPermissionCodesByRoleId(@PathVariable UUID roleId) {
        return rolePermUseCase.getPermissionCodesByRoleId(roleId);
    }
}
