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
 * Solo expone los endpoints {@code /system/internal/**} (S2S, no traversed por gateway).
 *
 * <p>El prefijo {@code /system} viene del {@code server.servlet.context-path}
 * configurado en system-service. Spring Boot lo despoja antes de matchear el
 * controller {@code /internal/**}.
 *
 * <p><b>Wire format</b>: usa {@code String} para los UUIDs (claves de Map y elementos
 * de Set). Asi evitamos depender de un {@code KeyDeserializer} de Jackson para UUID
 * en {@code Map<UUID,String>}, problema reproducible con Feign + Spring Cloud 2025.x.
 * Los adapters convierten UUID&lt;-&gt;String en el borde.
 */
@FeignClient(name = "system-service", contextId = "system-service-internal")
public interface SystemServiceClient {

    @PostMapping("/system/internal/roles/codes")
    Map<String, String> resolveRoleCodes(@RequestBody Set<String> roleIds);

    @GetMapping("/system/internal/roles/{roleId}/permissions/codes")
    Set<String> getPermissionCodesByRoleId(@PathVariable("roleId") UUID roleId);
}
