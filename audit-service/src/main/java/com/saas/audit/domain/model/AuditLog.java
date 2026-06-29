package com.saas.audit.domain.model;

import com.saas.common.audit.AuditAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro inmutable de auditoria. Una fila por cambio de dominio recibido
 * via {@code audit.events}.
 *
 * Vive en la tabla {@code audit_log} del esquema dedicado {@code saas_audit},
 * gobernado por Flyway en audit-service.
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "ix_audit_business", columnList = "BusinessId"),
        @Index(name = "ix_audit_aggregate", columnList = "AggregateType,AggregateId"),
        @Index(name = "ix_audit_actor", columnList = "ActorId"),
        @Index(name = "ix_audit_occurred", columnList = "OccurredAt"),
        @Index(name = "ux_audit_event", columnList = "EventId", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @Column(name = "Id", columnDefinition = "BINARY(16)")
    private UUID id;

    /** eventId del envelope: unico, garantiza idempotencia a nivel BD. */
    @Column(name = "EventId", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "Action", nullable = false, length = 16)
    private AuditAction action;

    @Column(name = "AggregateType", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "AggregateId", columnDefinition = "BINARY(16)")
    private UUID aggregateId;

    /** Null = auditoria a nivel sistema; presente = auditoria de un negocio. */
    @Column(name = "BusinessId", columnDefinition = "BINARY(16)")
    private UUID businessId;

    @Column(name = "ActorId", columnDefinition = "BINARY(16)")
    private UUID actorId;

    @Column(name = "ActorName", length = 128)
    private String actorName;

    @Column(name = "OccurredAt", nullable = false)
    private Instant occurredAt;

    @Column(name = "BeforeJson", columnDefinition = "JSON")
    private String beforeJson;

    @Column(name = "AfterJson", columnDefinition = "JSON")
    private String afterJson;

    /** Lista de campos cambiados, serializada a JSON (solo UPDATE/TOGGLE). */
    @Column(name = "ChangedFields", columnDefinition = "JSON")
    private String changedFields;

    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt;
}
