-- =====================================================================
-- V3__1.0.0.sql
-- VINCULACIONES DE USUARIO Y DISPOSITIVO ("usuarios_vinculaciones").
--
-- Va aparte de V1 por la misma razon que V2: llega despues de que el
-- esquema base ya este creado y con datos. Meterlo en V1 obligaria a
-- recrear el esquema entero — que en desarrollo se puede, pero se lleva
-- por delante los negocios, empleados y cargos de prueba sin que nadie
-- lo haya pedido. Cuando se consolide el esquema para produccion, este
-- bloque se mueve a V1 y este archivo desaparece.
-- =====================================================================

-- ---------------------------------------------------------------------
-- QUE ES Y QUE NO ES
--
-- Esta tabla NO es la de dispositivos de push (esa es `user_device`, en
-- saas_events, y guarda tokens de FCM que cambian solos). Esta responde
-- otra pregunta: "¿desde que telefono entra esta persona?".
--
-- La diferencia esta en DeviceId. Es el identificador interno que genera
-- el propio telefono y que la app guarda en el llavero del sistema
-- (Keystore/Keychain), no en sus datos: sobrevive a que se desinstale la
-- app, se borren sus datos o se limpie la cache. Un token de FCM, en
-- cambio, se regenera con cualquiera de esas tres cosas — por eso no
-- sirve para saber si es "el mismo aparato".
--
-- Con eso se cumplen las dos reglas que faltaban:
--   1. Una cuenta entra desde UN dispositivo a la vez. Si intenta entrar
--      desde otro, se le avisa y decide si desvincula el anterior.
--   2. Un dispositivo tiene UNA cuenta activa. Si entra otra persona en
--      el mismo telefono, se le avisa igual.
-- Ninguna de las dos bloquea: informan y piden confirmacion. Bloquear
-- dejaria fuera a quien cambia de telefono, que es un caso normal.
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS user_device_link (
    Id           CHAR(36)     NOT NULL,
    UserId       CHAR(36)     NOT NULL,

    -- Serial interno del aparato. Lo genera el telefono, no el servidor:
    -- el servidor no puede saber si dos peticiones vienen del mismo sitio.
    DeviceId     VARCHAR(128) NOT NULL,

    -- Como llamarlo al preguntarle a la persona: "Galaxy A54". Sin esto,
    -- el aviso diria "otro dispositivo" y nadie sabria cual.
    DeviceName   VARCHAR(120) NULL,
    Platform     VARCHAR(16)  NOT NULL DEFAULT 'ANDROID',
    AppVersion   VARCHAR(32)  NULL,

    -- Ultima vez que este par usuario+aparato hizo login. Es lo que se le
    -- muestra a quien tiene que decidir si desvincula ("visto hace 3 dias").
    LastSeenAt   DATETIME(6)  NOT NULL,

    -- NULL = vinculo ACTIVO. Se marca la fecha en vez de borrar la fila
    -- porque "desde que aparato entro y cuando dejo de hacerlo" es
    -- justamente el dato que hace falta si alguien reclama un acceso.
    RevokedAt    DATETIME(6)  NULL,
    -- Quien lo desvinculo: el propio usuario al entrar desde otro sitio,
    -- o un administrador. Vale NULL para los vinculos vivos.
    RevokedBy    CHAR(36)     NULL,

    Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy    CHAR(36)     NULL,
    AuditUser    CHAR(36)     NULL,
    AuditDate    DATETIME(6)  NOT NULL,
    CreatedDate  DATETIME(6)  NOT NULL,

    PRIMARY KEY (Id),

    -- Un par usuario+aparato es UNO, se haya revocado y vuelto a vincular
    -- veinte veces. Sin esta unica, cada login desde el mismo telefono
    -- dejaria una fila nueva y la tabla creceria sin fin.
    UNIQUE KEY uq_user_device_link (UserId, DeviceId),

    -- "¿Que cuenta hay activa en ESTE aparato?" — la consulta del caso 2.
    KEY ix_udl_device (DeviceId, RevokedAt),
    -- "¿Desde que aparatos esta entrando ESTA cuenta?" — la del caso 1.
    KEY ix_udl_user (UserId, RevokedAt),

    CONSTRAINT fk_udl_user FOREIGN KEY (UserId) REFERENCES app_user (Id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
