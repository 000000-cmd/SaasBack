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
import java.util.stream.Collectors;

/**
 * Endpoints S2S (service-to-service) consumidos por auth-service via Feign.
 * Sin {@code ApiResponse} envolvente, sin auth -- la red interna los protege.
 * En la SecurityConfig {@code /internal/**} esta permitAll.
 *
 * <p><b>Wire format</b>: los UUIDs se intercambian como {@code String} (no como
 * tipo UUID nativo) para evitar problemas de {@code KeyDeserializer} en Jackson
 * cuando los UUIDs son claves de Map. La conversion a {@code UUID} ocurre dentro
 * del controller.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final IRoleUseCase roleUseCase;
    private final IRolePermissionUseCase rolePermUseCase;

    @PostMapping("/roles/codes")
    public Map<String, String> resolveRoleCodes(@RequestBody Set<String> roleIds) {
        Set<UUID> uuids = roleIds.stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet());
        Map<String, String> result = new HashMap<>();
        for (Role r : roleUseCase.findByIds(uuids)) {
            result.put(r.getId().toString(), r.getCode());
        }
        return result;
    }

    @GetMapping("/roles/{roleId}/permissions/codes")
    public Set<String> getPermissionCodesByRoleId(@PathVariable UUID roleId) {
        return rolePermUseCase.getPermissionCodesByRoleId(roleId);
    }
}
