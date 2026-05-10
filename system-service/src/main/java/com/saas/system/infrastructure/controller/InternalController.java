package com.saas.system.infrastructure.controller;

import com.saas.system.application.dto.event.RoleEventPayload;
import com.saas.system.domain.model.Role;
import com.saas.system.domain.port.in.IRolePermissionUseCase;
import com.saas.system.domain.port.in.IRoleUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
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

    @GetMapping("/roles/all")
    public List<RoleEventPayload> listAllForReindex(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        log.info("Reindex fetch roles: page={} size={}", page, size);
        return roleUseCase.findAllPaged(page, size).stream()
                .map(RoleEventPayload::from)
                .toList();
    }

    @GetMapping("/roles/count")
    public Map<String, Long> countRoles() {
        return Map.of("total", roleUseCase.count());
    }
}