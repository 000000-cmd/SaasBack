package com.saas.audit.domain.repository;

import com.saas.audit.domain.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Acceso a la tabla {@code audit_log}. JpaSpecificationExecutor habilita los
 * filtros dinamicos (businessId, aggregateType, actor, fechas, accion).
 */
public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    boolean existsByEventId(UUID eventId);
}
