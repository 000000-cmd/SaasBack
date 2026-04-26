package com.saas.auth.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Feign client a system-service via Eureka (lb://system-service).
 * Solo expone los endpoints {@code /internal/**} (S2S, no traversed por gateway).
 */
@FeignClient(name = "system-service", contextId = "system-service-internal")
public interface SystemServiceClient {

    @PostMapping("/internal/roles/codes")
    Map<UUID, String> resolveRoleCodes(@RequestBody Set<UUID> roleIds);

    @GetMapping("/internal/roles/{roleId}/permissions/codes")
    Set<String> getPermissionCodesByRoleId(@PathVariable("roleId") UUID roleId);
}
