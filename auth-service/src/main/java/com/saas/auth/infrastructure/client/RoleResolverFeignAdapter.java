package com.saas.auth.infrastructure.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.saas.auth.domain.port.out.IRoleResolverPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resolucion de Role.Id -> Role.Code consultando system-service via Feign.
 *
 * <p>Cache Caffeine en memoria (5 min TTL) para evitar latencia y carga en logins
 * masivos.
 *
 * <p>Fail-graceful: si Feign cae o el rol no se puede resolver, devuelve
 * silenciosamente lo que tenga (set parcial / vacio). Login no se rompe;
 * el usuario tendra menos roles efectivos hasta que system-service vuelva.
 *
 * <p>El wire format con system-service usa {@code Map<String,String>} (UUIDs
 * stringificados) para evitar el bug de Jackson KeyDeserializer con UUID en Feign.
 */
@Slf4j
@Primary
@Component("roleResolverFeignAdapter")
@RequiredArgsConstructor
public class RoleResolverFeignAdapter implements IRoleResolverPort {

    private final SystemServiceClient client;

    private final Cache<UUID, String> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(1_000)
            .build();

    @Override
    public Set<String> resolveCodes(Set<UUID> roleIds) {
        return new HashSet<>(resolveCodeMap(roleIds).values());
    }

    @Override
    public Map<UUID, String> resolveCodeMap(Set<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return Map.of();

        Map<UUID, String> resolved = new HashMap<>();
        Set<UUID> missing = new HashSet<>();
        for (UUID id : roleIds) {
            String cached = cache.getIfPresent(id);
            if (cached != null) resolved.put(id, cached);
            else missing.add(id);
        }

        if (!missing.isEmpty()) {
            // Wire format: Set<String>. Convertimos UUID -> String al cruzar la frontera Feign.
            Set<String> missingAsStrings = missing.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toSet());
            log.info("Feign call -> system-service /internal/roles/codes con {} ids: {}",
                    missingAsStrings.size(), missingAsStrings);

            try {
                Map<String, String> fetched = client.resolveRoleCodes(missingAsStrings);
                log.info("Feign respondio con {} mappings: {}", fetched.size(), fetched);

                // Y al volver, String -> UUID.
                fetched.forEach((idStr, code) -> {
                    UUID id = UUID.fromString(idStr);
                    cache.put(id, code);
                    resolved.put(id, code);
                });
            } catch (Exception ex) {
                // Stack trace completo para diagnostico (antes solo se logueaba el mensaje).
                log.warn("Feign->system-service resolve roles fallo (degradacion silenciosa)", ex);
            }
        }

        return resolved;
    }
}
