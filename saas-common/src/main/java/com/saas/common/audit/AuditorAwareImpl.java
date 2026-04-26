package com.saas.common.audit;

import com.saas.common.security.IUserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Provee el UUID del usuario actualmente autenticado a Spring Data JPA Auditing.
 *
 * Estrategia de resolucion (en orden):
 *   1. Si el principal implementa {@link IUserPrincipal}, usa su {@code userId}.
 *   2. Si {@code Authentication.getName()} es un UUID parseable, lo usa.
 *   3. Sin contexto autenticado (Flyway, jobs, eventos del sistema): devuelve
 *      vacio, lo que permite que la columna AuditUser quede en NULL.
 */
@Slf4j
@Component("auditorAware")
public class AuditorAwareImpl implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof IUserPrincipal up && up.getUserId() != null) {
            return Optional.of(up.getUserId());
        }

        try {
            return Optional.of(UUID.fromString(auth.getName()));
        } catch (IllegalArgumentException ex) {
            log.trace("Authentication.name no es un UUID parseable: {}", auth.getName());
            return Optional.empty();
        }
    }
}
