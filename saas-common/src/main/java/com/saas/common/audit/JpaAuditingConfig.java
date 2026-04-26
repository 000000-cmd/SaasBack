package com.saas.common.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activa Spring Data JPA Auditing en cualquier microservicio que importe
 * saas-common (los Application classes ya escanean {@code com.saas.common}).
 *
 * Engancha {@link AuditorAwareImpl} para rellenar {@code AuditUser} con el
 * UUID del principal en SecurityContext, y {@code AuditDate}/{@code CreatedDate}
 * con el timestamp del sistema.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {
}
