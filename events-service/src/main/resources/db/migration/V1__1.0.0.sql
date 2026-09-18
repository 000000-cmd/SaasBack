-- =====================================================================
-- V1__1.0.0.sql  (esquema saas_events, gestionado por events-service)
-- Este servicio guarda lo que ya ocurrio: lo registra (auditoria) y lo
-- comunica (notificaciones). Aqui vive el registro; las tablas de
-- notificacion se anaden en la Fase B, en este mismo script.
-- La BD saas_events la crea init-scripts; el esquema lo gobierna Flyway aqui.
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

-- =====================================================================
-- NOTIFICACIONES
-- Parametro: un hueco reemplazable ({{USUARIO}}).
-- Plantilla: el cuerpo de un mensaje para UN canal.
-- Notificacion: el hecho de negocio; agrupa las plantillas de cada canal.
-- La FK vive en la plantilla y arranca NULL: primero se redacta, despues
-- se crea la notificacion y se asocia. Ese es el orden real de trabajo.
-- =====================================================================

CREATE TABLE notification (
    Id          CHAR(36)     NOT NULL,
    Code        VARCHAR(80)  NOT NULL,
    Name        VARCHAR(120) NOT NULL,
    Description VARCHAR(500) NULL,
    -- IsGlobal: sólo estas aparecen en "Lanzamiento global". Arranca en FALSE a
    -- proposito: una notificacion se vuelve lanzable a toda la base cuando
    -- alguien lo decide, nunca por defecto.
    IsGlobal    BOOLEAN      NOT NULL DEFAULT FALSE,
    Enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy   CHAR(36)     NULL,
    AuditUser   CHAR(36)     NULL,
    AuditDate   DATETIME(6)  NOT NULL,
    CreatedDate DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_notification_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE notification_parameter (
    Id          CHAR(36)     NOT NULL,
    Code        VARCHAR(80)  NOT NULL,
    Name        VARCHAR(120) NOT NULL,
    Description VARCHAR(500) NULL,
    Enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy   CHAR(36)     NULL,
    AuditUser   CHAR(36)     NULL,
    AuditDate   DATETIME(6)  NOT NULL,
    CreatedDate DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_notif_param_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- TypeCode guarda el codigo del catalogo notification_type SIN clave foranea:
-- ese catalogo vive en saas_db y lo sirve system-service. No es un descuido,
-- es la frontera entre servicios.
CREATE TABLE notification_template (
    Id             CHAR(36)     NOT NULL,
    Code           VARCHAR(80)  NOT NULL,
    Name           VARCHAR(120) NOT NULL,
    TypeCode       VARCHAR(40)  NOT NULL,
    Subject        VARCHAR(300) NULL,
    Body           LONGTEXT     NOT NULL,
    NotificationId CHAR(36)     NULL,
    Enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible        BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy      CHAR(36)     NULL,
    AuditUser      CHAR(36)     NULL,
    AuditDate      DATETIME(6)  NOT NULL,
    CreatedDate    DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_notif_tpl_code (Code),
    KEY ix_notif_tpl_notification (NotificationId),
    CONSTRAINT fk_notif_tpl_notification FOREIGN KEY (NotificationId)
        REFERENCES notification (Id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Fila unica (Id fijo). Columnas explicitas y no clave-valor: son campos con
-- tipos distintos y una clave-valor solo aplaza el casting a tiempo de lectura.
CREATE TABLE notification_setting (
    Id                CHAR(36)     NOT NULL,
    ApiKey            VARCHAR(200) NULL,
    FromName          VARCHAR(120) NULL,
    FromEmail         VARCHAR(200) NULL,
    ReplyTo           VARCHAR(200) NULL,
    TestMode          BOOLEAN      NOT NULL DEFAULT TRUE,
    TestRecipient     VARCHAR(200) NULL,
    SendingEnabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    MaxRetries        INT          NOT NULL DEFAULT 3,
    RetryDelaySeconds INT          NOT NULL DEFAULT 30,
    LogRetentionDays  INT          NOT NULL DEFAULT 90,
    MonthlyQuota      INT          NULL,
    QuotaMonthlyUsed  INT          NULL,
    QuotaDailyUsed    INT          NULL,
    RateLimit         INT          NULL,
    RateRemaining     INT          NULL,
    QuotaObservedAt   DATETIME(6)  NULL,
    Enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible           BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy         CHAR(36)     NULL,
    AuditUser         CHAR(36)     NULL,
    AuditDate         DATETIME(6)  NOT NULL,
    CreatedDate       DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- TestMode arranca en TRUE a proposito: una instalacion recien migrada NO debe
-- poder mandarle correo a un cliente real hasta que alguien lo decida.
INSERT INTO notification_setting
    (Id, TestMode, SendingEnabled, MaxRetries, RetryDelaySeconds, LogRetentionDays,
     Enabled, Visible, AuditDate, CreatedDate)
VALUES
    ('99990000-0000-0000-0000-000000000001', TRUE, TRUE, 3, 30, 90,
     TRUE, TRUE, NOW(6), NOW(6));

-- uq_log_event_template es la idempotencia de verdad: si Kafka reentrega el
-- mensaje, el segundo intento choca contra el indice y nadie recibe el correo
-- dos veces. EventId queda NULL en envios directos y pruebas; MySQL admite
-- multiples NULL en un indice unico, asi que esos no se bloquean entre si.
--
-- El DESTINATARIO forma parte de la clave. Sin el, una notificacion con dos
-- destinos (el correo del cliente y su telefono, por ejemplo) reventaba contra
-- el indice al escribir la segunda bitacora: el mismo evento y la misma
-- plantilla, pero mandados a sitios distintos, NO son el mismo mensaje.
CREATE TABLE notification_log (
    Id                CHAR(36)     NOT NULL,
    NotificationCode  VARCHAR(80)  NULL,
    TemplateCode      VARCHAR(80)  NULL,
    TemplateId        CHAR(36)     NULL,
    TypeCode          VARCHAR(40)  NOT NULL,
    Recipient         VARCHAR(320) NOT NULL,
    Subject           VARCHAR(300) NULL,
    Status            VARCHAR(20)  NOT NULL,
    ProviderMessageId VARCHAR(120) NULL,
    Error             VARCHAR(500) NULL,
    EventId           CHAR(36)     NULL,
    AttemptCount      INT          NOT NULL DEFAULT 1,
    SentAt            DATETIME(6)  NULL,
    Enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible           BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy         CHAR(36)     NULL,
    AuditUser         CHAR(36)     NULL,
    AuditDate         DATETIME(6)  NOT NULL,
    CreatedDate       DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_log_event_template (EventId, TemplateId, Recipient),
    KEY ix_log_created (CreatedDate),
    KEY ix_log_recipient (Recipient),
    KEY ix_log_status (Status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =====================================================================
-- BANDEJA Y DISPOSITIVOS
-- ---------------------------------------------------------------------
-- notification_inbox NO es un canal: es la superficie de LECTURA. Se escribe
-- una fila por destinatario en TODO envio, salga por correo, SMS, WhatsApp o
-- push. La campana de la web y la lista del movil leen esta misma tabla, asi
-- que no hay "push de web" y "push de movil" como cosas distintas: el registro
-- es uno y FCM es solo lo que hace vibrar el telefono.
-- =====================================================================

CREATE TABLE notification_inbox (
    Id               CHAR(36)     NOT NULL,
    -- A quien va. Es un tercero (cliente, empleado o dueno): es donde viven los
    -- medios de contacto. No hay FK: esa tabla esta en saas_db, otro servicio.
    ThirdPartyId     CHAR(36)     NOT NULL,
    NotificationCode VARCHAR(80)  NULL,
    TemplateCode     VARCHAR(80)  NULL,
    TypeCode         VARCHAR(40)  NULL,
    Title            VARCHAR(300) NULL,
    Body             LONGTEXT     NOT NULL,
    -- NULL = no leida. Se guarda el instante y no un booleano porque "cuando la
    -- vio" es un dato que siempre acaba haciendo falta y no cuesta nada.
    ReadAt           DATETIME(6)  NULL,
    Enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible          BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy        CHAR(36)     NULL,
    AuditUser        CHAR(36)     NULL,
    AuditDate        DATETIME(6)  NOT NULL,
    CreatedDate      DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    -- La consulta que importa es "cuantas no leidas tiene esta persona". Sin
    -- este indice se recorre la tabla entera en cada carga de la campana.
    KEY ix_inbox_owner (ThirdPartyId, ReadAt),
    KEY ix_inbox_created (CreatedDate)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Dispositivos para push. El token es UNICO porque el mismo telefono puede
-- cambiar de dueno (alguien cierra sesion y entra otro): al registrarlo se
-- REASIGNA, no se duplica. Un token que el proveedor rechace como invalido se
-- borra; si no, la lista de destinos se llena de fantasmas y cada envio gasta
-- una llamada por cada uno.
CREATE TABLE user_device (
    Id           CHAR(36)     NOT NULL,
    ThirdPartyId CHAR(36)     NOT NULL,
    FcmToken     VARCHAR(255) NOT NULL,
    Platform     VARCHAR(16)  NOT NULL,
    AppVersion   VARCHAR(32)  NULL,
    LastSeenAt   DATETIME(6)  NULL,
    Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy    CHAR(36)     NULL,
    AuditUser    CHAR(36)     NULL,
    AuditDate    DATETIME(6)  NOT NULL,
    CreatedDate  DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_device_token (FcmToken),
    KEY ix_device_owner (ThirdPartyId)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =====================================================================
-- VERIFICACION DE CONTACTO POR CODIGO (OTP)
-- ---------------------------------------------------------------------
-- Generico por construccion: el codigo sale por el MISMO ChannelSender que
-- cualquier otra notificacion, resolviendo una plantilla con {{CODIGO}}. Por eso
-- SMS y WhatsApp no necesitan desarrollo extra: el dia que exista su proveedor,
-- el OTP sale por ahi porque nunca supo por donde salia.
-- =====================================================================

CREATE TABLE verification_code (
    Id          CHAR(36)     NOT NULL,
    -- El destino tal cual: correo o telefono. Es lo que se verifica.
    Target      VARCHAR(320) NOT NULL,
    ChannelCode VARCHAR(40)  NOT NULL,
    -- Para que se pidio: CONTACT_VERIFY hoy, y deja sitio a otros usos.
    Purpose     VARCHAR(40)  NOT NULL,
    -- Se guarda el HASH, nunca el codigo: es un secreto de un solo uso y la
    -- tabla es tan legible como cualquier otra para quien tenga acceso a la BD.
    CodeHash    VARCHAR(128) NOT NULL,
    ExpiresAt   DATETIME(6)  NOT NULL,
    Attempts    INT          NOT NULL DEFAULT 0,
    MaxAttempts INT          NOT NULL DEFAULT 5,
    -- NULL = todavia utilizable. Se marca al acertar o al agotar los intentos.
    ConsumedAt  DATETIME(6)  NULL,
    -- Contacto concreto a marcar como verificado al acertar. Vive en saas_db.
    ContactId   CHAR(36)     NULL,
    Enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy   CHAR(36)     NULL,
    AuditUser   CHAR(36)     NULL,
    AuditDate   DATETIME(6)  NOT NULL,
    CreatedDate DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    -- Busqueda del codigo activo de un destino, y la purga por vencimiento.
    KEY ix_vcode_target (Target, Purpose, ConsumedAt),
    KEY ix_vcode_expires (ExpiresAt)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =====================================================================
-- SEMILLA DEL SISTEMA: la notificacion que transporta el codigo de
-- verificacion. Se siembra porque el OTP no puede depender de que alguien
-- se acuerde de crearla: sin esta fila, pedir un codigo no manda nada.
-- =====================================================================

INSERT INTO notification_parameter (Id, Code, Name, Description, Enabled, Visible, AuditDate, CreatedDate) VALUES
('99994000-0000-0000-0000-000000000001', 'CODIGO',  'Código de verificación', 'El código de un solo uso que se envía para verificar un contacto.', TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000002', 'MINUTOS', 'Minutos de validez',     'Cuántos minutos sigue siendo válido el código.',                    TRUE, TRUE, NOW(6), NOW(6));

INSERT INTO notification (Id, Code, Name, Description, IsGlobal, Enabled, Visible, AuditDate, CreatedDate) VALUES
('99995000-0000-0000-0000-000000000001', 'CONTACT_VERIFY', 'Verificación de contacto',
 'Lleva el código de un solo uso con el que alguien confirma que un correo o un teléfono es suyo.',
 FALSE, TRUE, TRUE, NOW(6), NOW(6));

-- Plantilla de correo. Cuando existan proveedores de SMS y WhatsApp bastara con
-- anadir una plantilla mas de ese tipo asociada a esta misma notificacion: el
-- servicio de verificacion no cambia, porque nunca supo por donde salia.
INSERT INTO notification_template
    (Id, Code, Name, TypeCode, Subject, Body, NotificationId, Enabled, Visible, AuditDate, CreatedDate)
VALUES
('99996000-0000-0000-0000-000000000001', 'CONTACT_VERIFY_EMAIL', 'Verificación de contacto (correo)', 'EMAIL',
 'Tu código es {{CODIGO}}',
 CONCAT('<div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;max-width:480px;margin:0 auto;padding:24px">',
        '<p style="font-size:15px;line-height:1.5">Usa este código para confirmar tu contacto:</p>',
        '<p style="font-size:34px;font-weight:700;letter-spacing:8px;margin:24px 0">{{CODIGO}}</p>',
        '<p style="font-size:14px;color:#666">Vence en {{MINUTOS}} minutos. Si no lo pediste, ignora este mensaje.</p>',
        '</div>'),
 '99995000-0000-0000-0000-000000000001', TRUE, TRUE, NOW(6), NOW(6));

-- =====================================================================
-- SEMILLA: los dos correos del dinero.
--
-- Se siembran por la misma razon que CONTACT_VERIFY: son parte del flujo, no
-- una campana que alguien configura. Si estas filas no existen, liquidar y
-- dispersar nomina funcionan igual pero el empleado no se entera, que es
-- exactamente el problema que vienen a resolver.
--
-- Los colores van EN HEXADECIMAL a proposito y SOLO aqui: un correo se abre en
-- Gmail o en Outlook, no dentro de la app, y ningun cliente de correo resuelve
-- variables CSS. Los tokens OKLCH siguen siendo la regla en la interfaz.
-- =====================================================================

INSERT INTO notification_parameter (Id, Code, Name, Description, Enabled, Visible, AuditDate, CreatedDate) VALUES
('99994000-0000-0000-0000-000000000010', 'EMPLEADO',    'Nombre del colaborador',   'A quien va dirigido el mensaje.',                             TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000011', 'NEGOCIO',     'Nombre del negocio',       'La empresa que liquida o dispersa.',                          TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000012', 'MONTO',       'Monto',                    'Importe principal del mensaje, ya formateado.',               TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000013', 'SERVICIOS',   'Servicios incluidos',      'Cuantos servicios entraron en la liquidacion.',               TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000014', 'SALDO',       'Saldo a favor',            'Lo que le queda acumulado tras el movimiento.',               TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000015', 'FECHA',       'Fecha del movimiento',     'Cuando ocurrio, ya formateada.',                              TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000016', 'PERIODO',     'Periodo de nomina',        'El periodo que cubre la dispersion.',                         TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000017', 'COMISION',    'Parte de comision',        'Cuanto del pago corresponde a servicios liquidados.',         TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000018', 'SUELDO_BASE', 'Parte de sueldo base',     'Cuanto del pago corresponde al sueldo base del periodo.',     TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000019', 'REFERENCIA',  'Referencia de la corrida', 'Constancia interna de la dispersion (no es un id bancario).', TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000020', 'CLIENTE',     'Nombre del cliente',       'A quien se le presto el servicio y se le factura.',           TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000021', 'SERVICIO',    'Servicio prestado',        'El nombre del servicio que se factura.',                      TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000022', 'FACTURA',     'Numero de factura',        'El consecutivo con el que se identifica el documento.',       TRUE, TRUE, NOW(6), NOW(6)),
('99994000-0000-0000-0000-000000000023', 'LINK',        'Enlace de descarga',       'URL publica que devuelve la factura en PDF.',                 TRUE, TRUE, NOW(6), NOW(6));

INSERT INTO notification (Id, Code, Name, Description, IsGlobal, Enabled, Visible, AuditDate, CreatedDate) VALUES
('99995000-0000-0000-0000-000000000002', 'SETTLEMENT_CONFIRMED', 'Liquidación confirmada',
 'Avisa al colaborador de que sus servicios fueron aprobados y el monto ya está en su saldo a favor.',
 FALSE, TRUE, TRUE, NOW(6), NOW(6)),
('99995000-0000-0000-0000-000000000003', 'PAYROLL_DISPERSED', 'Nómina dispersada',
 'Avisa al colaborador de que se le consignó la nómina. Lleva adjunto el extracto en PDF.',
 FALSE, TRUE, TRUE, NOW(6), NOW(6)),
('99995000-0000-0000-0000-000000000004', 'CLIENT_INVOICE', 'Factura del cliente',
 'Le manda al cliente la factura de su servicio: por correo con el PDF adjunto, por SMS o WhatsApp con el enlace de descarga.',
 FALSE, TRUE, TRUE, NOW(6), NOW(6));

INSERT INTO notification_template
    (Id, Code, Name, TypeCode, Subject, Body, NotificationId, Enabled, Visible, AuditDate, CreatedDate)
VALUES
('99996000-0000-0000-0000-000000000002', 'SETTLEMENT_CONFIRMED_EMAIL', 'Liquidación confirmada (correo)', 'EMAIL',
 'Se abonaron {{MONTO}} a tu saldo',
 CONCAT('<div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;background:#f8f9fa;padding:32px 16px">',
        '<div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e1e3e4">',
        '<div style="background:#8204be;padding:24px 28px">',
          '<p style="margin:0;color:#f5d9ff;font-size:12px;letter-spacing:.08em;text-transform:uppercase">Liquidación confirmada</p>',
          '<p style="margin:6px 0 0;color:#ffffff;font-size:20px;font-weight:600">{{NEGOCIO}}</p>',
        '</div>',
        '<div style="padding:28px">',
          '<p style="margin:0 0 4px;font-size:15px;color:#191c1d">Hola {{EMPLEADO}},</p>',
          '<p style="margin:0 0 24px;font-size:15px;line-height:1.55;color:#4e4353">',
            'Revisamos y aprobamos <strong>{{SERVICIOS}}</strong> de tus servicios. El monto ya está en tu saldo a favor.',
          '</p>',
          '<div style="background:#f5d9ff;border-radius:12px;padding:20px;text-align:center">',
            '<p style="margin:0;font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:#7000a6">Abonado a tu saldo</p>',
            '<p style="margin:8px 0 0;font-size:34px;font-weight:800;color:#30004a;letter-spacing:-.02em">{{MONTO}}</p>',
          '</div>',
          '<table style="width:100%;border-collapse:collapse;margin-top:20px;font-size:14px">',
            '<tr><td style="padding:10px 0;color:#4e4353;border-bottom:1px solid #edeeef">Servicios incluidos</td>',
                '<td style="padding:10px 0;text-align:right;color:#191c1d;font-weight:600;border-bottom:1px solid #edeeef">{{SERVICIOS}}</td></tr>',
            '<tr><td style="padding:10px 0;color:#4e4353;border-bottom:1px solid #edeeef">Fecha</td>',
                '<td style="padding:10px 0;text-align:right;color:#191c1d;font-weight:600;border-bottom:1px solid #edeeef">{{FECHA}}</td></tr>',
            '<tr><td style="padding:10px 0;color:#4e4353">Saldo a favor total</td>',
                '<td style="padding:10px 0;text-align:right;color:#8204be;font-weight:700">{{SALDO}}</td></tr>',
          '</table>',
          '<p style="margin:24px 0 0;font-size:13px;line-height:1.5;color:#807384">',
            'Este saldo se te consigna en la próxima dispersión de nómina. Puedes ver el detalle servicio por servicio desde la app.',
          '</p>',
        '</div></div></div>'),
 '99995000-0000-0000-0000-000000000002', TRUE, TRUE, NOW(6), NOW(6)),

('99996000-0000-0000-0000-000000000003', 'PAYROLL_DISPERSED_EMAIL', 'Nómina dispersada (correo)', 'EMAIL',
 'Te consignamos {{MONTO}} · {{PERIODO}}',
 CONCAT('<div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;background:#f8f9fa;padding:32px 16px">',
        '<div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e1e3e4">',
        '<div style="background:#006c46;padding:24px 28px">',
          '<p style="margin:0;color:#7bfabb;font-size:12px;letter-spacing:.08em;text-transform:uppercase">Nómina dispersada</p>',
          '<p style="margin:6px 0 0;color:#ffffff;font-size:20px;font-weight:600">{{NEGOCIO}} · {{PERIODO}}</p>',
        '</div>',
        '<div style="padding:28px">',
          '<p style="margin:0 0 4px;font-size:15px;color:#191c1d">Hola {{EMPLEADO}},</p>',
          '<p style="margin:0 0 24px;font-size:15px;line-height:1.55;color:#4e4353">',
            'Ya se dispersó la nómina del periodo. Esto es lo que se te consignó.',
          '</p>',
          '<div style="background:#f3f4f5;border-radius:12px;padding:20px;text-align:center">',
            '<p style="margin:0;font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:#4e4353">Total consignado</p>',
            '<p style="margin:8px 0 0;font-size:34px;font-weight:800;color:#00734b;letter-spacing:-.02em">{{MONTO}}</p>',
          '</div>',
          '<table style="width:100%;border-collapse:collapse;margin-top:20px;font-size:14px">',
            '<tr><td style="padding:10px 0;color:#4e4353;border-bottom:1px solid #edeeef">Comisión por servicios</td>',
                '<td style="padding:10px 0;text-align:right;color:#191c1d;font-weight:600;border-bottom:1px solid #edeeef">{{COMISION}}</td></tr>',
            '<tr><td style="padding:10px 0;color:#4e4353;border-bottom:1px solid #edeeef">Sueldo base</td>',
                '<td style="padding:10px 0;text-align:right;color:#191c1d;font-weight:600;border-bottom:1px solid #edeeef">{{SUELDO_BASE}}</td></tr>',
            '<tr><td style="padding:10px 0;color:#4e4353;border-bottom:1px solid #edeeef">Saldo a favor restante</td>',
                '<td style="padding:10px 0;text-align:right;color:#191c1d;font-weight:600;border-bottom:1px solid #edeeef">{{SALDO}}</td></tr>',
            '<tr><td style="padding:10px 0;color:#4e4353">Referencia</td>',
                '<td style="padding:10px 0;text-align:right;color:#807384;font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:13px">{{REFERENCIA}}</td></tr>',
          '</table>',
          '<div style="margin-top:24px;border:1px solid #d1c1d5;border-radius:12px;padding:16px">',
            '<p style="margin:0 0 6px;font-size:14px;font-weight:600;color:#191c1d">Tu extracto va adjunto</p>',
            '<p style="margin:0;font-size:13px;line-height:1.5;color:#4e4353">',
              'Abre el PDF de este correo para ver el detalle completo. Está protegido: ',
              'la <strong>contraseña es tu número de documento</strong>, sin puntos ni espacios.',
            '</p>',
          '</div>',
          '<p style="margin:20px 0 0;font-size:13px;line-height:1.5;color:#807384">',
            'También puedes consultarlo desde la app, donde cada movimiento tiene su comprobante.',
          '</p>',
        '</div></div></div>'),
 '99995000-0000-0000-0000-000000000003', TRUE, TRUE, NOW(6), NOW(6)),

-- ---------------------------------------------------------------------
-- LA FACTURA DEL CLIENTE, por sus tres canales.
--
-- El correo lleva el PDF ADJUNTO. SMS y WhatsApp llevan el ENLACE: en esos
-- canales no se puede mandar un fichero, y el enlace apunta a un endpoint del
-- back que genera la factura en el momento. Los tres dicen lo mismo con el
-- espacio que cada uno tiene.
-- ---------------------------------------------------------------------
('99996000-0000-0000-0000-000000000004', 'CLIENT_INVOICE_EMAIL', 'Factura del cliente (correo)', 'EMAIL',
 'Tu factura de {{NEGOCIO}} · {{FACTURA}}',
 CONCAT('<div style="font-family:Georgia,''Times New Roman'',serif;background:#f0f3ff;padding:40px 16px">',
        '<div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:2px;overflow:hidden;border:1px solid #d1c1d5">',
        -- Cabecera editorial: mucho aire y una sola tinta, como el documento.
        '<div style="padding:40px 40px 28px;border-bottom:1px solid #d1c1d5">',
          '<p style="margin:0;font-family:system-ui,-apple-system,Segoe UI,sans-serif;font-size:10px;letter-spacing:.22em;text-transform:uppercase;color:#8204be;font-weight:700">Gracias por tu visita</p>',
          '<p style="margin:14px 0 0;font-size:38px;line-height:1;font-weight:700;color:#8204be;letter-spacing:.06em">FACTURA</p>',
          '<p style="margin:10px 0 0;font-family:system-ui,-apple-system,Segoe UI,sans-serif;font-size:12px;color:#5f5e5e">N.° {{FACTURA}} · {{FECHA}}</p>',
        '</div>',
        '<div style="padding:32px 40px">',
          '<p style="margin:0 0 6px;font-size:17px;color:#151c27">Hola {{CLIENTE}},</p>',
          '<p style="margin:0 0 28px;font-family:system-ui,-apple-system,Segoe UI,sans-serif;font-size:14px;line-height:1.6;color:#4e4353">',
            'Aquí tienes la factura de tu servicio en <strong>{{NEGOCIO}}</strong>. ',
            'Va adjunta en PDF a este correo, y también puedes descargarla cuando quieras desde el botón de abajo.',
          '</p>',
          -- El detalle del servicio, con el importe destacado.
          '<table style="width:100%;border-collapse:collapse;font-family:system-ui,-apple-system,Segoe UI,sans-serif;font-size:14px">',
            '<tr><td style="padding:12px 0;color:#5f5e5e;border-bottom:1px solid #f0f3ff">Servicio</td>',
                '<td style="padding:12px 0;text-align:right;color:#151c27;font-weight:600;border-bottom:1px solid #f0f3ff">{{SERVICIO}}</td></tr>',
            '<tr><td style="padding:12px 0;color:#5f5e5e;border-bottom:1px solid #f0f3ff">Te atendió</td>',
                '<td style="padding:12px 0;text-align:right;color:#151c27;font-weight:600;border-bottom:1px solid #f0f3ff">{{EMPLEADO}}</td></tr>',
            '<tr><td style="padding:12px 0;color:#5f5e5e;border-bottom:1px solid #f0f3ff">Fecha</td>',
                '<td style="padding:12px 0;text-align:right;color:#151c27;font-weight:600;border-bottom:1px solid #f0f3ff">{{FECHA}}</td></tr>',
          '</table>',
          '<div style="background:#f8f2ff;padding:22px 24px;margin-top:24px;display:flex">',
            '<table style="width:100%;border-collapse:collapse">',
              '<tr>',
                '<td style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;font-size:11px;letter-spacing:.16em;text-transform:uppercase;color:#7000a6;font-weight:700">Total pagado</td>',
                '<td style="text-align:right;font-size:30px;font-weight:700;color:#8204be;letter-spacing:-.02em">{{MONTO}}</td>',
              '</tr>',
            '</table>',
          '</div>',
          '<div style="text-align:center;margin-top:32px">',
            '<a href="{{LINK}}" style="display:inline-block;background:#8204be;color:#ffffff;text-decoration:none;',
               'font-family:system-ui,-apple-system,Segoe UI,sans-serif;font-size:14px;font-weight:600;padding:14px 34px;border-radius:2px;letter-spacing:.04em">',
               'Descargar mi factura</a>',
          '</div>',
          '<p style="margin:28px 0 0;font-family:system-ui,-apple-system,Segoe UI,sans-serif;font-size:12px;line-height:1.6;color:#807384;text-align:center">',
            'Conserva este documento: es el soporte del servicio y del pago.<br/>Si algo no cuadra, respóndenos a este correo.',
          '</p>',
        '</div>',
        '<div style="padding:20px 40px;border-top:1px solid #d1c1d5;background:#f9f9ff">',
          '<p style="margin:0;font-family:system-ui,-apple-system,Segoe UI,sans-serif;font-size:10px;letter-spacing:.14em;text-transform:uppercase;color:#807384">{{NEGOCIO}} · Documento electrónico</p>',
        '</div>',
        '</div></div>'),
 '99995000-0000-0000-0000-000000000004', TRUE, TRUE, NOW(6), NOW(6)),

('99996000-0000-0000-0000-000000000005', 'CLIENT_INVOICE_SMS', 'Factura del cliente (SMS)', 'SMS',
 'Tu factura {{FACTURA}}',
 CONCAT('{{NEGOCIO}}: gracias por tu visita. Tu factura {{FACTURA}} por {{MONTO}} ',
        '({{SERVICIO}}, {{FECHA}}) está lista. Descárgala aquí: {{LINK}}'),
 '99995000-0000-0000-0000-000000000004', TRUE, TRUE, NOW(6), NOW(6)),

('99996000-0000-0000-0000-000000000006', 'CLIENT_INVOICE_WHATSAPP', 'Factura del cliente (WhatsApp)', 'WHATSAPP',
 'Tu factura {{FACTURA}}',
 CONCAT('Hola {{CLIENTE}} 👋\n\nGracias por venir a *{{NEGOCIO}}*.\n\n',
        '*Servicio:* {{SERVICIO}}\n*Te atendió:* {{EMPLEADO}}\n*Fecha:* {{FECHA}}\n*Total:* {{MONTO}}\n\n',
        'Descarga tu factura {{FACTURA}} aquí:\n{{LINK}}'),
 '99995000-0000-0000-0000-000000000004', TRUE, TRUE, NOW(6), NOW(6));

-- ===================================================================
-- Avisos de la agenda (ver scripts/sql/citas-notificaciones.sql).
-- ===================================================================

INSERT INTO notification_parameter (Id, Code, Name, Description, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), d.Code, d.Name, d.Descripcion, TRUE, TRUE, NOW(6), NOW(6)
FROM (
    SELECT 'HORA'   AS Code, 'Hora de la cita' AS Name,
           'La hora local del negocio, ya formateada ("09:00").' AS Descripcion
    UNION ALL
    SELECT 'MOTIVO', 'Motivo',
           'Por que se cancelo, tal y como lo escribio quien cancelo.'
) d
WHERE NOT EXISTS (SELECT 1 FROM notification_parameter p WHERE p.Code = d.Code);

INSERT INTO notification (Id, Code, Name, Description, IsGlobal, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), d.Code, d.Name, d.Descripcion, FALSE, TRUE, TRUE, NOW(6), NOW(6)
FROM (
    SELECT 'APPOINTMENT_CONFIRMED' AS Code, 'Cita confirmada' AS Name,
           'Le confirma al cliente su cita con el codigo para consultarla o cancelarla.' AS Descripcion
    UNION ALL
    SELECT 'APPOINTMENT_REMINDER', 'Recordatorio de cita',
           'Le recuerda al cliente su cita antes de la hora. Cuantas horas antes lo decide cada negocio.'
    UNION ALL
    SELECT 'APPOINTMENT_CANCELLED', 'Cita cancelada',
           'Avisa al cliente de que su cita ya no esta en pie, y por que.'
) d
WHERE NOT EXISTS (SELECT 1 FROM notification n WHERE n.Code = d.Code);

-- ---------------------------------------------------------------------
-- Plantillas.
--
-- Cada canal dice lo mismo con el espacio que tiene. El dato que NO puede
-- faltar en ninguno es el CÓDIGO: es lo único que le permite al cliente
-- volver a su cita sin tener cuenta.
-- ---------------------------------------------------------------------
-- La notificacion se enlaza POR CODIGO. Con el id escrito a mano, si ese id ya
-- era de otra cosa la plantilla acaba colgando de la notificacion equivocada y
-- nada avisa: se descubre cuando llega el mensaje que no era.
INSERT IGNORE INTO notification_template
    (Id, Code, Name, TypeCode, Subject, Body, NotificationId, Enabled, Visible, AuditDate, CreatedDate)
VALUES
(UUID(), 'APPOINTMENT_CONFIRMED_EMAIL', 'Cita confirmada (correo)', 'EMAIL',
 'Tu cita en {{NEGOCIO}} · {{FECHA}} {{HORA}}',
 CONCAT('<div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;background:#f8f9fa;padding:32px 16px">',
        '<div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e1e3e4">',
        '<div style="padding:32px 32px 24px">',
          '<p style="margin:0;font-size:11px;letter-spacing:.18em;text-transform:uppercase;color:#8204be;font-weight:700">Tu cita está confirmada</p>',
          '<p style="margin:12px 0 0;font-size:26px;line-height:1.2;font-weight:700;color:#151c27">{{FECHA}} a las {{HORA}}</p>',
          '<p style="margin:8px 0 0;font-size:15px;color:#5f5e5e">{{SERVICIO}} · con {{EMPLEADO}}</p>',
        '</div>',
        '<div style="padding:0 32px 8px">',
          '<div style="background:#f8f2ff;border-radius:12px;padding:20px 24px;text-align:center">',
            '<p style="margin:0;font-size:11px;letter-spacing:.16em;text-transform:uppercase;color:#7000a6;font-weight:700">Tu código</p>',
            '<p style="margin:8px 0 0;font-size:30px;font-weight:700;letter-spacing:.18em;color:#8204be">{{CODIGO}}</p>',
            '<p style="margin:10px 0 0;font-size:12px;color:#5f5e5e">Guárdalo: con él consultas o cancelas tu cita sin crear ninguna cuenta.</p>',
          '</div>',
        '</div>',
        '<div style="padding:24px 32px 32px;text-align:center">',
          '<a href="{{LINK}}" style="display:inline-block;background:#8204be;color:#ffffff;text-decoration:none;',
             'font-size:14px;font-weight:600;padding:14px 34px;border-radius:999px">Ver o cancelar mi cita</a>',
          '<p style="margin:20px 0 0;font-size:12px;line-height:1.6;color:#807384">',
            'Si no vas a poder venir, cancélala con tiempo: así otra persona puede tomar la hora.',
          '</p>',
        '</div>',
        '<div style="padding:18px 32px;border-top:1px solid #e1e3e4;background:#f9f9ff">',
          '<p style="margin:0;font-size:10px;letter-spacing:.14em;text-transform:uppercase;color:#807384">{{NEGOCIO}}</p>',
        '</div>',
        '</div></div>'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_CONFIRMED'), TRUE, TRUE, NOW(6), NOW(6)),

(UUID(), 'APPOINTMENT_CONFIRMED_WHATSAPP', 'Cita confirmada (WhatsApp)', 'WHATSAPP',
 'Tu cita en {{NEGOCIO}}',
 CONCAT('¡Listo, {{CLIENTE}}! 🗓️\n\nTu cita en *{{NEGOCIO}}* quedó confirmada.\n\n',
        '*Cuándo:* {{FECHA}} a las {{HORA}}\n*Qué:* {{SERVICIO}}\n*Con:* {{EMPLEADO}}\n\n',
        'Tu código es *{{CODIGO}}*. Con él puedes ver o cancelar tu cita aquí:\n{{LINK}}'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_CONFIRMED'), TRUE, TRUE, NOW(6), NOW(6)),

(UUID(), 'APPOINTMENT_CONFIRMED_SMS', 'Cita confirmada (SMS)', 'SMS',
 'Tu cita en {{NEGOCIO}}',
 CONCAT('{{NEGOCIO}}: tu cita quedó para el {{FECHA}} a las {{HORA}} ({{SERVICIO}}). ',
        'Código {{CODIGO}}. Consúltala o cancélala: {{LINK}}'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_CONFIRMED'), TRUE, TRUE, NOW(6), NOW(6)),

-- El recordatorio no repite todo: quien lo lee ya sabe qué pidió. Dice cuándo
-- es y da la salida para cancelar, que es la acción que de verdad hace falta a
-- estas alturas.
(UUID(), 'APPOINTMENT_REMINDER_WHATSAPP', 'Recordatorio (WhatsApp)', 'WHATSAPP',
 'Recordatorio de tu cita',
 CONCAT('Hola {{CLIENTE}} 👋\n\nTe recordamos tu cita en *{{NEGOCIO}}*:\n\n',
        '*{{FECHA}} a las {{HORA}}*\n{{SERVICIO}} · con {{EMPLEADO}}\n\n',
        '¿No vas a poder? Cancélala aquí para liberar la hora:\n{{LINK}}'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_REMINDER'), TRUE, TRUE, NOW(6), NOW(6)),

(UUID(), 'APPOINTMENT_REMINDER_EMAIL', 'Recordatorio (correo)', 'EMAIL',
 'Recordatorio: tu cita en {{NEGOCIO}} es el {{FECHA}}',
 CONCAT('<div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;background:#f8f9fa;padding:32px 16px">',
        '<div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:16px;padding:32px;border:1px solid #e1e3e4">',
          '<p style="margin:0;font-size:11px;letter-spacing:.18em;text-transform:uppercase;color:#8204be;font-weight:700">Te esperamos</p>',
          '<p style="margin:12px 0 0;font-size:24px;line-height:1.2;font-weight:700;color:#151c27">{{FECHA}} a las {{HORA}}</p>',
          '<p style="margin:8px 0 0;font-size:15px;color:#5f5e5e">{{SERVICIO}} · con {{EMPLEADO}} · {{NEGOCIO}}</p>',
          '<p style="margin:24px 0 0;font-size:13px;line-height:1.6;color:#5f5e5e">',
            'Si no vas a poder venir, cancélala con tu código <strong>{{CODIGO}}</strong> ',
            'para que otra persona pueda tomar la hora.',
          '</p>',
          '<div style="margin-top:24px">',
            '<a href="{{LINK}}" style="display:inline-block;background:#8204be;color:#ffffff;text-decoration:none;',
               'font-size:14px;font-weight:600;padding:12px 28px;border-radius:999px">Ver mi cita</a>',
          '</div>',
        '</div></div>'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_REMINDER'), TRUE, TRUE, NOW(6), NOW(6)),

-- Cancelar SIEMPRE dice por qué, aunque el motivo venga vacío: un aviso de que
-- algo se cayó sin explicación deja al cliente llamando por teléfono.
(UUID(), 'APPOINTMENT_CANCELLED_WHATSAPP', 'Cita cancelada (WhatsApp)', 'WHATSAPP',
 'Tu cita quedó cancelada',
 CONCAT('Hola {{CLIENTE}}:\n\nTu cita en *{{NEGOCIO}}* del {{FECHA}} a las {{HORA}} quedó cancelada.\n\n',
        '*Motivo:* {{MOTIVO}}\n\nCuando quieras puedes reservar otra hora aquí:\n{{LINK}}'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_CANCELLED'), TRUE, TRUE, NOW(6), NOW(6)),

(UUID(), 'APPOINTMENT_CANCELLED_EMAIL', 'Cita cancelada (correo)', 'EMAIL',
 'Tu cita en {{NEGOCIO}} del {{FECHA}} quedó cancelada',
 CONCAT('<div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;background:#f8f9fa;padding:32px 16px">',
        '<div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:16px;padding:32px;border:1px solid #e1e3e4">',
          '<p style="margin:0;font-size:11px;letter-spacing:.18em;text-transform:uppercase;color:#7a2c2c;font-weight:700">Cita cancelada</p>',
          '<p style="margin:12px 0 0;font-size:22px;line-height:1.3;font-weight:700;color:#151c27">{{FECHA}} a las {{HORA}}</p>',
          '<p style="margin:8px 0 0;font-size:15px;color:#5f5e5e">{{SERVICIO}} · {{NEGOCIO}}</p>',
          '<div style="margin-top:20px;background:#fdf2f2;border-radius:12px;padding:16px 20px">',
            '<p style="margin:0;font-size:11px;letter-spacing:.14em;text-transform:uppercase;color:#7a2c2c;font-weight:700">Motivo</p>',
            '<p style="margin:6px 0 0;font-size:14px;color:#151c27">{{MOTIVO}}</p>',
          '</div>',
          '<div style="margin-top:24px">',
            '<a href="{{LINK}}" style="display:inline-block;background:#8204be;color:#ffffff;text-decoration:none;',
               'font-size:14px;font-weight:600;padding:12px 28px;border-radius:999px">Reservar otra hora</a>',
          '</div>',
        '</div></div>'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_CANCELLED'), TRUE, TRUE, NOW(6), NOW(6));

-- ===================================================================
-- WhatsApp (ver scripts/sql/whatsapp*.sql).
-- ===================================================================

INSERT INTO notification (Id, Code, Name, Description, IsGlobal, Enabled, Visible,
                          AuditDate, CreatedDate)
SELECT UUID(), 'WHATSAPP_REPLY', 'Respuesta del bot de WhatsApp',
       'Lo que el asistente contesta en la conversacion de reserva. El texto lo compone el flujo.',
       FALSE, TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification n WHERE n.Code = 'WHATSAPP_REPLY');

INSERT INTO notification_parameter (Id, Code, Name, Description, Enabled, Visible,
                                    AuditDate, CreatedDate)
SELECT UUID(), 'MENSAJE', 'Mensaje',
       'El texto ya compuesto por la conversacion.', TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification_parameter p WHERE p.Code = 'MENSAJE');

INSERT IGNORE INTO notification_template
    (Id, Code, Name, TypeCode, Subject, Body, NotificationId, Enabled, Visible,
     AuditDate, CreatedDate)
VALUES
(UUID(), 'WHATSAPP_REPLY_WHATSAPP', 'Respuesta del bot (WhatsApp)', 'WHATSAPP',
 'Reserva', '{{MENSAJE}}',
 (SELECT Id FROM notification WHERE Code = 'WHATSAPP_REPLY'), TRUE, TRUE, NOW(6), NOW(6));
