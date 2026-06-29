-- =====================================================================
-- V1__1.0.0.sql  (esquema saas_audit, gestionado por audit-service)
-- Tabla unica y modular de auditoria. UUID en BINARY(16), payloads JSON.
-- La BD saas_audit la crea init-scripts; el esquema lo gobierna Flyway aqui.
-- =====================================================================

CREATE TABLE audit_log (
    Id            BINARY(16)   NOT NULL,
    EventId       BINARY(16)   NOT NULL,
    Action        VARCHAR(16)  NOT NULL,
    AggregateType VARCHAR(64)  NOT NULL,
    AggregateId   BINARY(16)   NULL,
    BusinessId    BINARY(16)   NULL,
    ActorId       BINARY(16)   NULL,
    ActorName     VARCHAR(128) NULL,
    OccurredAt    DATETIME(6)  NOT NULL,
    BeforeJson    JSON         NULL,
    AfterJson     JSON         NULL,
    ChangedFields JSON         NULL,
    CreatedAt     DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY ux_audit_event (EventId),
    KEY ix_audit_business (BusinessId),
    KEY ix_audit_aggregate (AggregateType, AggregateId),
    KEY ix_audit_actor (ActorId),
    KEY ix_audit_occurred (OccurredAt)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
