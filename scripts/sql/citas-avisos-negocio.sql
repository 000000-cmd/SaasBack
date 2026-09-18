-- ---------------------------------------------------------------------------
-- Lo que la AGENDA necesita para no avisar dos veces.
--
-- `appointment_notification` es la deduplicación que pide la especificación:
-- una fila por (cita, tipo de aviso), con clave única. Si el barrido de
-- recordatorios corre dos veces —o dos instancias corren a la vez—, el segundo
-- INSERT choca contra el índice y no sale un segundo mensaje. Que la
-- idempotencia la garantice la base y no un `if` es lo que la hace cierta
-- también cuando hay dos procesos.
--
-- Es una tabla y no una columna por tipo porque cuántos recordatorios manda un
-- negocio es CONFIGURABLE: con columnas, añadir "también 2 horas antes" sería
-- una migración.
-- ---------------------------------------------------------------------------

USE saas_db;

-- MySQL 8.4 no admite `ADD COLUMN IF NOT EXISTS`; se comprueba antes.
SET @hay := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'business_booking_policy'
               AND COLUMN_NAME = 'ReminderHoursBefore');
SET @ddl := IF(@hay = 0,
    'ALTER TABLE business_booking_policy ADD COLUMN ReminderHoursBefore VARCHAR(40) NOT NULL DEFAULT ''24'' COMMENT ''Horas antes a las que se recuerda, separadas por coma ("24,2"). Vacio = no recordar.'' AFTER WhatsappCountResetAt',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS appointment_notification (
    Id               CHAR(36)    NOT NULL,
    AppointmentId    CHAR(36)    NOT NULL,
    -- El codigo de la notificacion en events-service (APPOINTMENT_REMINDER...).
    -- Para los recordatorios lleva ademas las horas ("APPOINTMENT_REMINDER:24"),
    -- porque el de 24 horas y el de 2 son avisos distintos.
    NotificationCode VARCHAR(80) NOT NULL,
    SentAt           DATETIME(6) NOT NULL,
    Enabled          BOOLEAN     NOT NULL DEFAULT TRUE,
    Visible          BOOLEAN     NOT NULL DEFAULT TRUE,
    CreatedBy        CHAR(36)    NULL,
    AuditUser        CHAR(36)    NULL,
    AuditDate        DATETIME(6) NOT NULL,
    CreatedDate      DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_appt_notif (AppointmentId, NotificationCode),
    KEY idx_appt_notif_appointment (AppointmentId),
    CONSTRAINT fk_appt_notif_appointment FOREIGN KEY (AppointmentId) REFERENCES appointment (Id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
