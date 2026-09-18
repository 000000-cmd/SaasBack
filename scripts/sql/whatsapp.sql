-- ---------------------------------------------------------------------------
-- WhatsApp: lo que entra y la conversacion en curso.
--
-- Dos tablas y ninguna mas. La de SALIDA no hace falta: los avisos ya viajan
-- por el outbox de eventos hasta events-service, que resuelve la plantilla y
-- envia. Una segunda cola solo para WhatsApp seria un segundo sitio donde se
-- pierden mensajes.
-- ---------------------------------------------------------------------------

USE saas_db;

-- ---------------------------------------------------------------------
-- LO QUE ENTRA
--
-- Se guarda el payload CRUDO antes de interpretarlo. Cuando una conversacion
-- se tuerce, lo unico que permite saber que dijo Meta de verdad es esto; el
-- objeto ya interpretado solo dice lo que entendimos.
--
-- `WaMessageId` es UNICO y es la deduplicacion. Meta REENTREGA los webhooks
-- —lo dice su documentacion— y sin esta clave una reentrega crearia una
-- segunda cita. Es idempotencia por indice, no por un `if`: dos entregas
-- simultaneas pasarian las dos cualquier comprobacion previa.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS whatsapp_message (
    Id           CHAR(36)     NOT NULL,
    -- El id que pone Meta. Nulo imposible: sin el no hay dedup.
    WaMessageId  VARCHAR(120) NOT NULL,
    -- El negocio se resuelve por el numero al que escribieron. Nulo mientras
    -- no se sepa: el mensaje se guarda igual, que es de lo que se trata.
    BusinessId   CHAR(36)     NULL,
    FromPhone    VARCHAR(24)  NOT NULL,
    ToPhone      VARCHAR(24)  NULL,
    Body         TEXT         NULL,
    RawPayload   MEDIUMTEXT   NOT NULL,
    ReceivedAt   DATETIME(6)  NOT NULL,
    -- Nulo = todavia no se pudo interpretar. Se deja a proposito para que un
    -- fallo quede a la vista y se pueda reintentar, en vez de desaparecer.
    ProcessedAt  DATETIME(6)  NULL,
    Error        VARCHAR(500) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_wa_message (WaMessageId),
    KEY idx_wa_from (FromPhone, ReceivedAt),
    KEY idx_wa_pendiente (ProcessedAt)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- LA CONVERSACION EN CURSO
--
-- Una por (negocio, telefono). Guarda EN QUE PASO va y lo que lleva
-- recogido, en JSON, porque los pasos y los campos los decide la
-- configuracion del flujo: una columna por dato obligaria a migrar la tabla
-- cada vez que alguien anada una pregunta.
--
-- `ExpiresAt` no es limpieza: una conversacion a medias de hace tres dias no
-- es la misma conversacion. Sin caducidad, quien escribe "hola" despues de
-- una semana se encuentra contestando la pregunta cuatro.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS whatsapp_session (
    Id          CHAR(36)     NOT NULL,
    BusinessId  CHAR(36)     NOT NULL,
    PhoneE164   VARCHAR(24)  NOT NULL,
    -- Que flujo se esta recorriendo. Hoy siempre AGEND; la columna existe
    -- porque el dia que haya otro (reprogramar, por ejemplo) no hay migracion.
    FlowCode    VARCHAR(40)  NOT NULL DEFAULT 'AGEND',
    -- El codigo de la seccion en la que esta. NULL = sin empezar.
    StepCode    VARCHAR(40)  NULL,
    DataJson    JSON         NULL,
    ExpiresAt   DATETIME(6)  NOT NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_wa_session (BusinessId, PhoneE164),
    KEY idx_wa_session_expira (ExpiresAt),
    CONSTRAINT fk_wa_session_business FOREIGN KEY (BusinessId) REFERENCES business (Id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- El numero de WhatsApp de cada negocio.
--
-- Es como se resuelve a quien le escribieron: el webhook trae el numero de
-- destino, no el negocio. Va en la politica de reservas, junto al interruptor
-- que ya existia para apagar el canal entero.
-- ---------------------------------------------------------------------
SET @hay := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'business_booking_policy'
               AND COLUMN_NAME = 'WhatsappPhoneId');
SET @ddl := IF(@hay = 0,
    'ALTER TABLE business_booking_policy ADD COLUMN WhatsappPhoneId VARCHAR(40) NULL COMMENT ''Id del numero de WhatsApp Business de este negocio (lo da Meta)'' AFTER WhatsappEnabled',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @hay2 := (SELECT COUNT(*) FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'business_booking_policy'
                AND INDEX_NAME = 'uq_bbp_wa_phone');
SET @ddl2 := IF(@hay2 = 0,
    'ALTER TABLE business_booking_policy ADD UNIQUE KEY uq_bbp_wa_phone (WhatsappPhoneId)',
    'DO 0');
PREPARE stmt2 FROM @ddl2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
