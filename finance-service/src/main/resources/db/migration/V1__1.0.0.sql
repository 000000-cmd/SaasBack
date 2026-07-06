-- =====================================================================
-- V1__1.0.0.sql  (esquema saas_finance, gestionado por finance-service)
-- Base del microservicio financiero. La BD saas_finance la crea
-- init-scripts (o createDatabaseIfNotExist); el esquema lo gobierna
-- Flyway desde aqui, igual que auth-service y audit-service con los suyos.
--
-- Estandar BD de la plataforma:
--   * Tablas: snake_case singular    * Columnas: PascalCase
--   * PK de dominio: Id CHAR(36) (UUID generado por la app)
--   * Auditoria de dominio: Enabled, Visible, AuditUser, AuditDate, CreatedDate
--   * InnoDB / utf8mb4_unicode_ci
--
-- De momento esta migracion solo levanta la infraestructura transversal
-- (outbox). Las tablas de dominio financiero se agregaran en Vx sucesivas.
-- =====================================================================

-- ---------------------------------------------------------------------
-- TABLA: outbox_event  (Transactional Outbox Pattern)
-- ---------------------------------------------------------------------
-- finance-service es PRODUCTOR de eventos: cada cambio de dominio que deba
-- notificarse escribe una fila aqui DENTRO DE LA MISMA TRANSACCION del
-- cambio. El OutboxRelay (saas-common, scheduled) lee las PENDING, las
-- publica a Kafka y las marca PUBLISHED. Definicion identica a la del
-- esquema saas_db para que la entidad OutboxEvent de saas-common valide.
-- ---------------------------------------------------------------------
CREATE TABLE outbox_event (
    Id            BINARY(16)    NOT NULL,
    EventId       BINARY(16)    NOT NULL,
    AggregateType VARCHAR(64)   NOT NULL,
    AggregateId   BINARY(16)    NOT NULL,
    EventType     VARCHAR(128)  NOT NULL,
    Version       INT           NOT NULL DEFAULT 1,
    BusinessId    BINARY(16)    NULL,
    Payload       JSON          NOT NULL,
    Status        VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    Retries       INT           NOT NULL DEFAULT 0,
    LastError     TEXT          NULL,
    CreatedAt     DATETIME(3)   NOT NULL,
    PublishedAt   DATETIME(3)   NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uk_outbox_event_id (EventId),
    INDEX idx_outbox_status_created (Status, CreatedAt)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
