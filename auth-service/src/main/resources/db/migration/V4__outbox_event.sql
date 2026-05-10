-- =====================================================================
-- TABLA: outbox_event
-- ---------------------------------------------------------------------
-- Implementa el "Transactional Outbox Pattern" para garantizar consistencia
-- entre cambios de BD y publicacion de eventos a Kafka.
--
-- Cada cambio de dominio que deba notificarse a otros servicios escribe
-- una fila en esta tabla DENTRO DE LA MISMA TRANSACCION del cambio.
-- Un componente "OutboxRelay" (scheduled) lee filas PENDING, las publica
-- a Kafka y las marca PUBLISHED.
--
-- Atomicidad: garantizada por MySQL (mismo TX que el cambio de dominio).
-- Compartida entre auth-service y system-service: ambos escriben aqui.
-- =====================================================================

CREATE TABLE outbox_event (
    -- PK del registro outbox (no del evento). UUID en BINARY(16) por eficiencia.
        Id              BINARY(16)    NOT NULL,

    -- Id unico del evento de dominio. Lo usan los consumers para
    -- deduplicacion. Aunque OutboxRelay publique 2 veces (retry), va con
    -- el mismo EventId.
        EventId         BINARY(16)    NOT NULL,

    -- "user", "role", "menu", "payroll-run", ...
        AggregateType   VARCHAR(64)   NOT NULL,

    -- Id de la entidad afectada (userId, roleId, payrollRunId).
        AggregateId     BINARY(16)    NOT NULL,

    -- "user.created", "user.updated", "role.deleted", ...
        EventType       VARCHAR(128)  NOT NULL,

    -- Version del payload (no del envelope). Default 1.
        Version         INT           NOT NULL  DEFAULT 1,

    -- Multi-tenancy. Nullable hasta que exista la entidad Business.
        BusinessId      BINARY(16)    NULL,

    -- Payload del evento serializado a JSON.
        Payload         JSON          NOT NULL,

    -- Estado del envio: PENDING, PUBLISHED, FAILED.
       Status          VARCHAR(16)   NOT NULL  DEFAULT 'PENDING',

    -- Numero de reintentos cuando publicacion falla.
       Retries         INT           NOT NULL  DEFAULT 0,

    -- Mensaje del ultimo error (truncado a 1000 chars en codigo).
       LastError       TEXT          NULL,

    -- Cuando se creo el evento (instante del cambio de dominio).
       CreatedAt       DATETIME(3)   NOT NULL,

    -- Cuando se publico exitosamente (NULL si todavia PENDING).
       PublishedAt     DATETIME(3)   NULL,

       PRIMARY KEY (Id),

    -- EventId debe ser unico globalmente. Si por bug se intenta insertar
    -- el mismo eventId 2 veces, MySQL lo bloquea.
       UNIQUE KEY uk_outbox_event_id (EventId),

    -- Indice para que el relay haga "WHERE Status='PENDING' ORDER BY CreatedAt"
    -- de manera eficiente. Sin indice seria full-scan (lento con miles de rows).
                              INDEX idx_outbox_status_created (Status, CreatedAt)
);