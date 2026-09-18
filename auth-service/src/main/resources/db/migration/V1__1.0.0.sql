-- =====================================================================
-- V1__1.0.0.sql
-- Schema + seed unificado de la plataforma SaaS (version de app 1.0.0).
--
--
-- Consolida el historico completo: V1__schema, V2__seed_base, V3__realign_menus,
-- V4__outbox_event, V6__business_schema, third_party, y las que fueron V3..V12
-- (landing del negocio, altas minimas de empleado, saldo, especialidades,
-- salario base hibrido, liquidaciones, tour guiado, gestion de Elastic,
-- "Mi empresa" y la bandera de submenu).
--
-- Al consolidar, cada ALTER posterior se aplico sobre el CREATE que le
-- correspondia y cada UPDATE de seed se resolvio al valor final: aqui no hay
-- columnas que nazcan NOT NULL para relajarse tres lineas mas abajo, ni menus
-- que se inserten para apagarse despues. Lo que se lee es el estado final.
--
-- Estandar BD:
--   * Tablas: snake_case singular   * Columnas: PascalCase
--   * PK: Id CHAR(36) (UUID app)     * Enabled/Visible/AuditUser/AuditDate/CreatedDate
--   * InnoDB / utf8mb4_unicode_ci
-- =====================================================================

-- ===================== [schema base] =====================
-- =====================================================================
-- V1__schema.sql
-- Schema unificado SaaS Platform.
-- Estandar:
--   * Tablas: snake_case singular
--   * Columnas: PascalCase
--   * PK: Id CHAR(36) (UUID generado por la aplicacion)
--   * Toda tabla incluye: Enabled, Visible, AuditUser, AuditDate, CreatedDate
-- =====================================================================

-- ---------------------------------------------------------------------
-- AUTH DOMAIN (auth-service)
-- ---------------------------------------------------------------------

-- =====================================================================
-- DOS ESQUEMAS, UNA FRONTERA
-- ---------------------------------------------------------------------
-- `personas` guarda QUIEN ES ALGUIEN: su documento, su nombre, sus
-- contactos, sus direcciones y sus cuentas bancarias. Nada mas.
--
-- `saas_db` guarda el NEGOCIO: empresas, sedes, empleos, agenda, dinero
-- y configuracion. Referencia a las personas por id, nunca copia sus
-- datos.
--
-- Por que separarlos y no dejar todo junto:
--   1. El dato personal es el que tiene reglas propias (quien lo ve, cuanto
--      se guarda, como se borra). Con las tablas mezcladas, "damelo todo de
--      esta persona" y "borra sus datos" son consultas imposibles de acotar.
--   2. La regla de privacidad del marketplace —un negocio ve a SU cliente,
--      nunca su actividad en otro— deja de depender de que alguien se
--      acuerde de filtrar: para llegar a una persona hay que pasar por
--      `business_client`, y esa tabla ya esta acotada al negocio.
--   3. Un permiso de solo lectura sobre `personas` es una linea, no una
--      lista de tablas que hay que mantener.
--
-- NO hay claves foraneas entre los dos esquemas. Es la misma frontera que
-- ya existe con `saas_events`: las referencias cruzadas van como columnas
-- indexadas y las valida la aplicacion. Una FK cruzada ata el ciclo de
-- vida de dos servicios que se despliegan por separado.
-- =====================================================================
CREATE SCHEMA IF NOT EXISTS personas
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE app_user (
    Id              CHAR(36)     NOT NULL,
    Username        VARCHAR(60)  NOT NULL,
    Email           VARCHAR(120) NOT NULL,
    PasswordHash    VARCHAR(120) NOT NULL,
    -- Nombres NULL a proposito: la cuenta del empleado nace minima (usuario,
    -- correo y contrasena) cuando el dueno lo da de alta, y el propio empleado
    -- completa sus datos desde el APK.
    FirstName       VARCHAR(80)  NULL,
    LastName        VARCHAR(80)  NULL,
    ProfilePhoto    VARCHAR(500) NULL,
    Theme           VARCHAR(30)  NOT NULL DEFAULT 'light',
    LanguageCode    VARCHAR(10)  NOT NULL DEFAULT 'es-CO',
    LastLoginAt     DATETIME(6)  NULL,
    -- TRUE hasta que el tercero ve el modal de bienvenida por primera vez.
    IsFirstLogin    BOOLEAN      NOT NULL DEFAULT TRUE,
    Enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible         BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate       DATETIME(6)  NOT NULL,
    CreatedDate     DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_app_user_username (Username),
    UNIQUE KEY uq_app_user_email (Email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_role (
    Id          CHAR(36)    NOT NULL,
    UserId      CHAR(36)    NOT NULL,
    RoleId      CHAR(36)    NOT NULL,
    Enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN     NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate   DATETIME(6) NOT NULL,
    CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_user_role (UserId, RoleId),
    KEY idx_user_role_user (UserId),
    KEY idx_user_role_role (RoleId),
    CONSTRAINT fk_user_role_user FOREIGN KEY (UserId) REFERENCES app_user (Id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE refresh_token (
    Id          CHAR(36)     NOT NULL,
    UserId      CHAR(36)     NOT NULL,
    Token       VARCHAR(500) NOT NULL,
    ExpiresAt   DATETIME(6)  NOT NULL,
    RevokedAt   DATETIME(6)  NULL,
    Enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate   DATETIME(6)  NOT NULL,
    CreatedDate DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_refresh_token_token (Token),
    KEY idx_refresh_token_user (UserId),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (UserId) REFERENCES app_user (Id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- SYSTEM DOMAIN (system-service)
-- ---------------------------------------------------------------------

CREATE TABLE role (
    Id          CHAR(36)     NOT NULL,
    Code        VARCHAR(50)  NOT NULL,
    Name        VARCHAR(120) NOT NULL,
    Description VARCHAR(500) NULL,
    Enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate   DATETIME(6)  NOT NULL,
    CreatedDate DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_role_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE permission (
    Id          CHAR(36)     NOT NULL,
    Code        VARCHAR(50)  NOT NULL,
    Name        VARCHAR(120) NOT NULL,
    Description VARCHAR(500) NULL,
    Enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate   DATETIME(6)  NOT NULL,
    CreatedDate DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_permission_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE role_permission (
    Id           CHAR(36)    NOT NULL,
    RoleId       CHAR(36)    NOT NULL,
    PermissionId CHAR(36)    NOT NULL,
    Enabled      BOOLEAN     NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN     NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate    DATETIME(6) NOT NULL,
    CreatedDate  DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_role_permission (RoleId, PermissionId),
    KEY idx_role_permission_role (RoleId),
    KEY idx_role_permission_permission (PermissionId),
    CONSTRAINT fk_role_permission_role
        FOREIGN KEY (RoleId) REFERENCES role (Id),
    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (PermissionId) REFERENCES permission (Id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE menu (
    Id           CHAR(36)     NOT NULL,
    Code         VARCHAR(50)  NOT NULL,
    Name         VARCHAR(120) NOT NULL,
    Icon         VARCHAR(60)  NULL,
    Route        VARCHAR(200) NULL,
    ParentId     CHAR(36)     NULL,
    DisplayOrder INT          NOT NULL DEFAULT 0,
    -- Un submenu NO se pinta en el carril lateral: se pinta en el nav
    -- secundario de su padre (el dock de "Mi empresa"). Por eso el padre es
    -- obligatorio para un submenu, y lo valida MenuService: sin padre no hay
    -- donde pintarlo. DEFAULT b'0' = menu normal.
    IsSubmenu    BIT(1)       NOT NULL DEFAULT b'0',
    Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate    DATETIME(6)  NOT NULL,
    CreatedDate  DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_menu_code (Code),
    KEY idx_menu_parent (ParentId),
    CONSTRAINT fk_menu_parent FOREIGN KEY (ParentId) REFERENCES menu (Id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE menu_role (
    Id          CHAR(36)    NOT NULL,
    MenuId      CHAR(36)    NOT NULL,
    RoleId      CHAR(36)    NOT NULL,
    Enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN     NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate   DATETIME(6) NOT NULL,
    CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_menu_role (MenuId, RoleId),
    KEY idx_menu_role_menu (MenuId),
    KEY idx_menu_role_role (RoleId),
    CONSTRAINT fk_menu_role_menu FOREIGN KEY (MenuId) REFERENCES menu (Id),
    CONSTRAINT fk_menu_role_role FOREIGN KEY (RoleId) REFERENCES role (Id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE system_list (
    Id          CHAR(36)     NOT NULL,
    Code        VARCHAR(80)  NOT NULL,
    Name        VARCHAR(120) NOT NULL,
    Description VARCHAR(500) NULL,
    Enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate   DATETIME(6)  NOT NULL,
    CreatedDate DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_system_list_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Tablas de catalogo: misma estructura, una por catalogo (semanticamente independientes).
-- Se acceden via /list/{nombre_tabla} usando CatalogController + CatalogRegistry.
CREATE TABLE document_type (
    Id           CHAR(36)     NOT NULL,
    Code         VARCHAR(80)  NOT NULL,
    Name         VARCHAR(120) NOT NULL,
    Value        VARCHAR(500) NULL,
    DisplayOrder INT          NOT NULL DEFAULT 0,
    Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate    DATETIME(6)  NOT NULL,
    CreatedDate  DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_document_type_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE registration_status (
    Id           CHAR(36)     NOT NULL,
    Code         VARCHAR(80)  NOT NULL,
    Name         VARCHAR(120) NOT NULL,
    Value        VARCHAR(500) NULL,
    DisplayOrder INT          NOT NULL DEFAULT 0,
    Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate    DATETIME(6)  NOT NULL,
    CreatedDate  DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_registration_status_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE gender (
    Id           CHAR(36)     NOT NULL,
    Code         VARCHAR(80)  NOT NULL,
    Name         VARCHAR(120) NOT NULL,
    Value        VARCHAR(500) NULL,
    DisplayOrder INT          NOT NULL DEFAULT 0,
    Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate    DATETIME(6)  NOT NULL,
    CreatedDate  DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_gender_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Catalogo de canales de notificacion (EMAIL, WHATSAPP, SMS). Vive en saas_db
-- porque los catalogos son de system-service; events-service (saas_events)
-- solo guarda el Code como texto suelto (ver notification_template.TypeCode).
CREATE TABLE notification_type (
    Id           CHAR(36)     NOT NULL,
    Code         VARCHAR(80)  NOT NULL,
    Name         VARCHAR(120) NOT NULL,
    Value        VARCHAR(255) NULL,
    DisplayOrder INT          NOT NULL DEFAULT 0,
    Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy    CHAR(36)     NULL,
    AuditUser    CHAR(36)     NULL,
    AuditDate    DATETIME(6)  NOT NULL,
    CreatedDate  DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_notification_type_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE constant (
    Id          CHAR(36)      NOT NULL,
    Code        VARCHAR(80)   NOT NULL,
    Name        VARCHAR(120)  NOT NULL,
    Value       VARCHAR(1000) NOT NULL,
    Description VARCHAR(500)  NULL,
    Enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN       NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate   DATETIME(6)   NOT NULL,
    CreatedDate DATETIME(6)   NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_constant_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ===================== [outbox] =====================
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
-- ===================== [business catalogs] =====================
CREATE TABLE business_type (
                                Id           CHAR(36)     NOT NULL,
                                Code         VARCHAR(80)  NOT NULL,
                                Name         VARCHAR(120) NOT NULL,
                                Value        VARCHAR(500) NULL,
                                DisplayOrder INT          NOT NULL DEFAULT 0,
                                Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
                                Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
                                CreatedBy CHAR(36) NULL,
                                AuditUser CHAR(36) NULL,
                                AuditDate    DATETIME(6)  NOT NULL,
                                CreatedDate  DATETIME(6)  NOT NULL,
                                PRIMARY KEY (Id),
                                UNIQUE KEY uq_business_type_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE branch_type (
                               Id           CHAR(36)     NOT NULL,
                               Code         VARCHAR(80)  NOT NULL,
                               Name         VARCHAR(120) NOT NULL,
                               Value        VARCHAR(500) NULL,
                               DisplayOrder INT          NOT NULL DEFAULT 0,
                               Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
                               Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
                               CreatedBy CHAR(36) NULL,
                               AuditUser CHAR(36) NULL,
                               AuditDate    DATETIME(6)  NOT NULL,
                               CreatedDate  DATETIME(6)  NOT NULL,
                               PRIMARY KEY (Id),
                               UNIQUE KEY uq_branch_type_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE contact_type (
                               Id           CHAR(36)     NOT NULL,
                               Code         VARCHAR(80)  NOT NULL,
                               Name         VARCHAR(120) NOT NULL,
                               Value        VARCHAR(500) NULL,
                               DisplayOrder INT          NOT NULL DEFAULT 0,
                               Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
                               Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
                               CreatedBy CHAR(36) NULL,
                               AuditUser CHAR(36) NULL,
                               AuditDate    DATETIME(6)  NOT NULL,
                               CreatedDate  DATETIME(6)  NOT NULL,
                               PRIMARY KEY (Id),
                               UNIQUE KEY uq_contact_type_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE status (
                              Id           CHAR(36)     NOT NULL,
                              Code         VARCHAR(80)  NOT NULL,
                              Name         VARCHAR(120) NOT NULL,
                              Value        VARCHAR(500) NULL,
                              DisplayOrder INT          NOT NULL DEFAULT 0,
                              Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
                              Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
                              CreatedBy CHAR(36) NULL,
                              AuditUser CHAR(36) NULL,
                              AuditDate    DATETIME(6)  NOT NULL,
                              CreatedDate  DATETIME(6)  NOT NULL,
                              PRIMARY KEY (Id),
                              UNIQUE KEY uq_status_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE schedule_status (
                              Id           CHAR(36)     NOT NULL,
                              Code         VARCHAR(80)  NOT NULL,
                              Name         VARCHAR(120) NOT NULL,
                              Value        VARCHAR(500) NULL,
                              DisplayOrder INT          NOT NULL DEFAULT 0,
                              Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
                              Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
                              CreatedBy CHAR(36) NULL,
                              AuditUser CHAR(36) NULL,
                              AuditDate    DATETIME(6)  NOT NULL,
                              CreatedDate  DATETIME(6)  NOT NULL,
                              PRIMARY KEY (Id),
                              UNIQUE KEY uq_schedule_status_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
-- ---------------------------------------------------------------------
-- SEED: registro en system_list de los catalogos de negocio.
-- Codes coinciden con el getCatalogPath() de cada *Service, asi el
-- frontend usa el Code de system_list para llamar /list/{Code}.
-- ---------------------------------------------------------------------
SET @now_v6 = NOW(6);

INSERT INTO system_list (Id, Code, Name, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('44444444-0000-0000-0000-000000000004', 'business_type',        'Tipos de Negocio',        'Categorias de negocio (peluqueria, spa, barberia, etc.)', TRUE, TRUE, NULL, @now_v6, @now_v6),
    ('44444444-0000-0000-0000-000000000005', 'branch_type',       'Tipos de Sucursal',       'Clasificacion de sucursales',                              TRUE, TRUE, NULL, @now_v6, @now_v6),
    ('44444444-0000-0000-0000-000000000006', 'contact_type',       'Tipos de Contacto',       'Medios de contacto (telefono, email, whatsapp, etc.)',     TRUE, TRUE, NULL, @now_v6, @now_v6),
    ('44444444-0000-0000-0000-000000000007', 'status',              'Estados',                 'Estados genericos para entidades de negocio',              TRUE, TRUE, NULL, @now_v6, @now_v6),
    ('44444444-0000-0000-0000-000000000008', 'schedule_status', 'Estados de Agendamiento', 'Estados del ciclo de vida de una cita',                    TRUE, TRUE, NULL, @now_v6, @now_v6);

-- ---------------------------------------------------------------------
-- THIRDPARTY DOMAIN (thirdparty-service)
-- ---------------------------------------------------------------------

CREATE TABLE personas.third_party (
    Id             CHAR(36)     NOT NULL,
    -- Documento NULL a proposito: el alta minima de empleado crea el tercero
    -- como "shell" (solo sus FKs) y el empleado completa el documento desde
    -- el APK. La unicidad (tipo, numero) sigue valiendo cuando ya hay datos.
    DocumentTypeId CHAR(36)     NULL,
    DocumentNumber VARCHAR(40)  NULL,
    UserId         CHAR(36)     NULL,
    FirstName      VARCHAR(80)  NULL,
    SecondName     VARCHAR(80)  NULL,
    FirstLastName  VARCHAR(80)  NULL,
    SecondLastName VARCHAR(80)  NULL,
    GenderId       CHAR(36)     NULL,
    BirthDate      DATE         NULL,
    PhotoUrl       VARCHAR(500) NULL,
    -- Habilita el ingreso con huella en el APK (la huella se valida en el
    -- dispositivo; aqui solo queda el consentimiento/estado del tercero).
    BiometricEnabled BOOLEAN    NOT NULL DEFAULT FALSE,
    Enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible        BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL,
    AuditUser CHAR(36) NULL,
    AuditDate      DATETIME(6)  NOT NULL,
    CreatedDate    DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_third_party_document (DocumentTypeId, DocumentNumber),
    KEY idx_third_party_user (UserId),
    KEY idx_third_party_doc_type (DocumentTypeId),
    KEY idx_third_party_gender (GenderId)
    -- Sin FK: los catalogos viven en saas_db y los gobierna
    -- system-service. Es la frontera entre esquemas, no un descuido.
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ===================== [seed base] =====================
-- =====================================================================
-- V2__seed_base.sql
-- Datos iniciales (linea base configurable):
--   * Roles base: ADMIN, USER, GUEST
--   * Permisos base: VIEW, CREATE, EDIT, DELETE, EXPORT, IMPORT
--   * Asignaciones role_permission por defecto
--   * Listas del sistema: TIPOS_DOCUMENTO, ESTADOS_REGISTRO, GENEROS
--   * Constantes ejemplo: MAYORIA_EDAD, MAX_LOGIN_ATTEMPTS, SESION_TIMEOUT_MIN
--   * Menus admin (/admin/*), uno por pantalla real
--   * El usuario administrador se inserta desde codigo (DataInitializer)
--     porque requiere bcrypt en runtime.
-- =====================================================================

SET @now = NOW(6);

-- ---------------------------------------------------------------------
-- ROLES
-- ---------------------------------------------------------------------
INSERT INTO role (Id, Code, Name, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('11111111-0000-0000-0000-000000000001', 'ADMIN', 'Administrador',  'Acceso total al sistema',         TRUE, TRUE, NULL, @now, @now),
    ('11111111-0000-0000-0000-000000000002', 'USER',  'Usuario',        'Usuario estandar autenticado',    TRUE, TRUE, NULL, @now, @now),
    ('11111111-0000-0000-0000-000000000003', 'GUEST', 'Invitado',       'Acceso limitado de solo lectura', TRUE, TRUE, NULL, @now, @now),
    ('11111111-0000-0000-0000-000000000004', 'OWNER', 'Dueño',          'Dueño de un negocio (tenant)',    TRUE, TRUE, NULL, @now, @now),
    ('11111111-0000-0000-0000-000000000005', 'EMPLOYEE', 'Empleado',    'Empleado de un negocio (solo APK movil)', TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- PERMISSIONS
-- ---------------------------------------------------------------------
INSERT INTO permission (Id, Code, Name, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('22222222-0000-0000-0000-000000000001', 'VIEW',   'Ver',       'Permite consultar registros',  TRUE, TRUE, NULL, @now, @now),
    ('22222222-0000-0000-0000-000000000002', 'CREATE', 'Crear',     'Permite crear registros',      TRUE, TRUE, NULL, @now, @now),
    ('22222222-0000-0000-0000-000000000003', 'EDIT',   'Editar',    'Permite editar registros',     TRUE, TRUE, NULL, @now, @now),
    ('22222222-0000-0000-0000-000000000004', 'DELETE', 'Eliminar',  'Permite eliminar registros',   TRUE, TRUE, NULL, @now, @now),
    ('22222222-0000-0000-0000-000000000005', 'EXPORT', 'Exportar',  'Permite exportar informacion', TRUE, TRUE, NULL, @now, @now),
    ('22222222-0000-0000-0000-000000000006', 'IMPORT', 'Importar',  'Permite importar informacion', TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- ROLE_PERMISSION
--   ADMIN -> todos
--   USER  -> VIEW, CREATE, EDIT
--   GUEST -> VIEW
-- ---------------------------------------------------------------------
INSERT INTO role_permission (Id, RoleId, PermissionId, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    -- ADMIN
    ('33333333-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000002', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000003', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000004', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000004', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000005', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000005', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000006', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000006', TRUE, TRUE, NULL, @now, @now),
    -- USER
    ('33333333-0000-0000-0000-000000000010', '11111111-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000011', '11111111-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000002', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000012', '11111111-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000003', TRUE, TRUE, NULL, @now, @now),
    -- GUEST
    ('33333333-0000-0000-0000-000000000020', '11111111-0000-0000-0000-000000000003', '22222222-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    -- OWNER -> gestion completa de su negocio (rol fijo referenciado por auth-service al registrar un dueño)
    ('33333333-0000-0000-0000-000000000041', '11111111-0000-0000-0000-000000000004', '22222222-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000042', '11111111-0000-0000-0000-000000000004', '22222222-0000-0000-0000-000000000002', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000043', '11111111-0000-0000-0000-000000000004', '22222222-0000-0000-0000-000000000003', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000044', '11111111-0000-0000-0000-000000000004', '22222222-0000-0000-0000-000000000004', TRUE, TRUE, NULL, @now, @now),
    -- EMPLOYEE -> operacion basica desde el APK (rol fijo referenciado al crear empleados)
    ('33333333-0000-0000-0000-000000000051', '11111111-0000-0000-0000-000000000005', '22222222-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000052', '11111111-0000-0000-0000-000000000005', '22222222-0000-0000-0000-000000000002', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-000000000053', '11111111-0000-0000-0000-000000000005', '22222222-0000-0000-0000-000000000003', TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- SYSTEM LISTS (catalogos configurables)
-- ---------------------------------------------------------------------
INSERT INTO system_list (Id, Code, Name, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('44444444-0000-0000-0000-000000000001', 'document_type',  'Tipos de Documento',          'Catalogo de tipos de documento de identidad', TRUE, TRUE, NULL, @now, @now),
    ('44444444-0000-0000-0000-000000000002', 'registration_status', 'Estados de Registro',         'Estados genericos para entidades del sistema', TRUE, TRUE, NULL, @now, @now),
    ('44444444-0000-0000-0000-000000000003', 'gender',          'Generos',                     'Catalogo de generos',                          TRUE, TRUE, NULL, @now, @now),
    ('99991001-0000-0000-0000-000000000001', 'notification-type', 'Tipos de notificación', 'Canales por los que se puede enviar una notificación.', TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- CATALOGOS (cada uno en su propia tabla, acceso via /list/{tabla})
-- ---------------------------------------------------------------------
INSERT INTO document_type (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('55555555-0000-0000-0000-000000000001', 'CC',  'Cedula de Ciudadania',  'CC',  1, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000002', 'TI',  'Tarjeta de Identidad',  'TI',  2, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000003', 'CE',  'Cedula de Extranjeria', 'CE',  3, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000004', 'PA',  'Pasaporte',             'PA',  4, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000005', 'NIT', 'NIT',                   'NIT', 5, TRUE, TRUE, NULL, @now, @now);

INSERT INTO registration_status (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('55555555-0000-0000-0000-000000000010', 'ACT', 'Activo',    'ACTIVE',   1, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000011', 'INA', 'Inactivo',  'INACTIVE', 2, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000012', 'PEN', 'Pendiente', 'PENDING',  3, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000013', 'BLO', 'Bloqueado', 'BLOCKED',  4, TRUE, TRUE, NULL, @now, @now);

INSERT INTO gender (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('55555555-0000-0000-0000-000000000020', 'M', 'Masculino', 'M', 1, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000021', 'F', 'Femenino',  'F', 2, TRUE, TRUE, NULL, @now, @now),
    ('55555555-0000-0000-0000-000000000022', 'O', 'Otro',      'O', 3, TRUE, TRUE, NULL, @now, @now);

INSERT INTO notification_type (Id, Code, Name, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('99991000-0000-0000-0000-000000000001', 'EMAIL',    'Correo electrónico', 1, TRUE, TRUE, NULL, @now, @now),
    ('99991000-0000-0000-0000-000000000002', 'WHATSAPP', 'WhatsApp',           2, TRUE, TRUE, NULL, @now, @now),
    ('99991000-0000-0000-0000-000000000003', 'SMS',      'Mensaje de texto',   3, TRUE, TRUE, NULL, @now, @now),
    -- PUSH es UN solo canal: la notificacion del sistema operativo en el
    -- telefono. Lo que se ve en la web es la BANDEJA, que no es un canal sino la
    -- superficie de lectura y se escribe en todo envio, salga por donde salga.
    ('99991000-0000-0000-0000-000000000004', 'PUSH',     'Notificación al teléfono', 4, TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- CONSTANTS (valores de configuracion globales, todos como STRING)
-- ---------------------------------------------------------------------
INSERT INTO constant (Id, Code, Name, Value, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('66666666-0000-0000-0000-000000000001', 'MAYORIA_EDAD',        'Mayoria de edad',                  '18',  'Edad minima para ser mayor de edad',                              TRUE, TRUE, NULL, @now, @now),
    ('66666666-0000-0000-0000-000000000002', 'MAX_LOGIN_ATTEMPTS',  'Maximo intentos de login',         '5',   'Cantidad maxima de intentos fallidos antes de bloquear cuenta',   TRUE, TRUE, NULL, @now, @now),
    ('66666666-0000-0000-0000-000000000003', 'SESION_TIMEOUT_MIN',  'Timeout de sesion (minutos)',      '60',  'Tiempo de inactividad antes de cerrar sesion automaticamente',    TRUE, TRUE, NULL, @now, @now),
    ('66666666-0000-0000-0000-000000000004', 'PROFILE_PHOTO_MAX_KB', 'Tamano maximo foto de perfil (KB)', '512', 'Limite de tamano para subir foto de perfil de usuario',           TRUE, TRUE, NULL, @now, @now),
    -- Debe ir a la par de AppInfo.version del APK: es contra esta constante que
    -- el APK decide si exige actualizar. Publicar un APK desde el admin la
    -- reescribe; este valor es solo la linea base de una instalacion nueva.
    ('66666666-0000-0000-0000-000000000005', 'VERAPP',              'Version vigente del APK',          '1.0.3', 'Si la version instalada difiere, el APK exige actualizar',        TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- MENUS (estructura jerarquica configurable)
--   Padre sin ParentId  = seccion principal
--   Hijo con ParentId   = sub-seccion
-- ---------------------------------------------------------------------
-- ===================== [menus admin] =====================
-- Solo menus de administracion (/admin/*). Cada Route corresponde a una ruta
-- real de admin.routes.ts; no se siembran menus sin pantalla.

-- ---------------------------------------------------------------------
-- 1. MENUS ADMIN
-- ---------------------------------------------------------------------
INSERT INTO menu (
    Id, Code, Name, Icon, Route, ParentId,
    DisplayOrder, Enabled, Visible,
    AuditUser, AuditDate, CreatedDate
) VALUES

('77770001-0000-0000-0000-000000000001', 'ADMIN_DASHBOARD', 'Panel', 'activity', '/admin/dashboard', NULL, 1, TRUE, TRUE, NULL, @now, @now),

('77770001-0000-0000-0000-000000000010', 'ADMIN_USERS_GROUP', 'Usuarios', 'users', NULL, NULL, 2, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000011', 'ADMIN_USERS', 'Todos los usuarios', 'users', '/admin/users', '77770001-0000-0000-0000-000000000010', 1, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000012', 'ADMIN_INVITES', 'Invitaciones', 'user-plus', '/admin/invitations', '77770001-0000-0000-0000-000000000010', 2, TRUE, TRUE, NULL, @now, @now),

('77770001-0000-0000-0000-000000000050', 'ADMIN_THIRDPARTY', 'Terceros', 'contact', '/admin/thirdparty', NULL, 3, TRUE, TRUE, NULL, @now, @now),
('77770001-0000-0000-0000-000000000060', 'ADMIN_BUSINESS', 'Empresas', 'building-2', '/admin/business', NULL, 4, TRUE, TRUE, NULL, @now, @now),

('77770001-0000-0000-0000-000000000020', 'ADMIN_SYSTEM_GROUP', 'Sistema', 'settings', NULL, NULL, 5, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000021', 'ADMIN_LISTS', 'Listas', 'list-tree', '/admin/system-lists', '77770001-0000-0000-0000-000000000020', 1, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000022', 'ADMIN_CONSTANTS', 'Constantes', 'hash', '/admin/constants', '77770001-0000-0000-0000-000000000020', 2, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000023', 'ADMIN_MENUS', 'Menus', 'menu', '/admin/menus', '77770001-0000-0000-0000-000000000020', 3, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000024', 'ADMIN_POLITICAL_DIVISION', 'Division politica', 'map', '/admin/political-division', '77770001-0000-0000-0000-000000000020', 4, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000025', 'ADMIN_SYSTEM_STATUS', 'Estado del sistema', 'server', '/admin/system-status', '77770001-0000-0000-0000-000000000020', 5, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000026', 'ADMIN_APP_VERSIONS', 'Versiones del APK', 'smartphone', '/admin/app-versions', '77770001-0000-0000-0000-000000000020', 6, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000027', 'ADMIN_TOUR', 'Tour guiado', 'compass', '/admin/tour', '77770001-0000-0000-0000-000000000020', 7, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000028', 'ADMIN_ELASTIC', 'Gestion de Elastic', 'database', '/admin/elastic', '77770001-0000-0000-0000-000000000020', 8, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000029', 'ADMIN_FLOWS', 'Flujos', 'workflow', '/admin/flows', '77770001-0000-0000-0000-000000000020', 9, TRUE, TRUE, NULL, @now, @now),

('77770001-0000-0000-0000-000000000030', 'ADMIN_SECURITY_GROUP', 'Seguridad', 'shield', NULL, NULL, 6, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000031', 'ADMIN_ROLES', 'Roles', 'shield', '/admin/roles', '77770001-0000-0000-0000-000000000030', 1, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000032', 'ADMIN_PERMS', 'Permisos', 'shield-check', '/admin/permissions', '77770001-0000-0000-0000-000000000030', 2, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000033', 'ADMIN_AUDIT', 'Auditoria', 'scroll-text', '/admin/audit', '77770001-0000-0000-0000-000000000030', 3, TRUE, TRUE, NULL, @now, @now),

('77770001-0000-0000-0000-000000000040', 'ADMIN_PREFS_GROUP', 'Preferencias', 'settings', NULL, NULL, 9, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000041', 'ADMIN_PROFILE', 'Mi perfil', 'user', '/admin/profile', '77770001-0000-0000-0000-000000000040', 1, TRUE, TRUE, NULL, @now, @now)

ON DUPLICATE KEY UPDATE
                     Name = VALUES(Name),
                     Icon = VALUES(Icon),
                     Route = VALUES(Route),
                     ParentId = VALUES(ParentId),
                     DisplayOrder = VALUES(DisplayOrder),
                     Enabled = VALUES(Enabled),
                     Visible = VALUES(Visible),
                     AuditDate = VALUES(AuditDate);

-- ---------------------------------------------------------------------
-- 2. MENU_ROLE - ADMIN (idempotente)
-- El filtro va por Code, no solo por familia de Id: "Mi empresa" nacio con un
-- Id de la familia 77770002 (venia de una migracion posterior) y sin este
-- AND el administrador heredaria un menu que es del dueno.
-- ---------------------------------------------------------------------
INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE (m.Id LIKE '77770001-0000-%' OR m.Id LIKE '77770002-0000-%')
  AND m.Code LIKE 'ADMIN%'
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr
    WHERE mr.MenuId = m.Id
      AND mr.RoleId = '11111111-0000-0000-0000-000000000001'
);

-- ---------------------------------------------------------------------
-- 3. MENU ADMIN - NOTIFICACIONES (Fase B). El padre ADMIN_NOTIFY va antes que
-- sus tres hijos: fk_menu_parent se valida fila a fila dentro del INSERT.
-- Los Id son de la familia 99993000, fuera de '77770001'/'77770002', asi que
-- el filtro generico de arriba (bloque 2) no los alcanza: llevan su propio
-- menu_role para ADMIN.
-- ---------------------------------------------------------------------
INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder, IsSubmenu, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
('99993000-0000-0000-0000-000000000001', 'ADMIN_NOTIFY',        'Notificaciones', 'bell',      '/admin/notificaciones',            NULL,                                     10, b'0', TRUE, TRUE, NULL, @now, @now),
('99993000-0000-0000-0000-000000000002', 'ADMIN_NOTIFY_PARAMS', 'Parámetros',     'braces',    '/admin/notificaciones/parametros', '99993000-0000-0000-0000-000000000001',  1, b'1', TRUE, TRUE, NULL, @now, @now),
('99993000-0000-0000-0000-000000000003', 'ADMIN_NOTIFY_TPL',    'Plantillas',     'file-text', '/admin/notificaciones/plantillas', '99993000-0000-0000-0000-000000000001',  2, b'1', TRUE, TRUE, NULL, @now, @now),
('99993000-0000-0000-0000-000000000004', 'ADMIN_NOTIFY_LIST',   'Notificaciones', 'send',      '/admin/notificaciones/lista',      '99993000-0000-0000-0000-000000000001',  3, b'1', TRUE, TRUE, NULL, @now, @now),
('99993000-0000-0000-0000-000000000005', 'ADMIN_NOTIFY_GLOBAL', 'Lanzamiento global', 'megaphone', '/admin/notificaciones/global',   '99993000-0000-0000-0000-000000000001',  4, b'1', TRUE, TRUE, NULL, @now, @now);

INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE m.Code LIKE 'ADMIN_NOTIFY%'
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr
    WHERE mr.MenuId = m.Id
      AND mr.RoleId = '11111111-0000-0000-0000-000000000001'
);

-- ===================== [menus tenant - OWNER] =====================
-- Menus del dueño (rol OWNER). Apuntan a rutas /tenant/* (área separada de /admin).
-- El sidebar del dueño se nutre de estos via /menus/me (config por rol).
--
-- Estructura final:
--   Panel
--   Negocio  (grupo, sin ruta)
--     |- Mi empresa    -> pantalla con dock lateral; sus seis hijos son
--     |                   SUBMENUS: no se pintan en el carril lateral sino
--     |                   en ese dock (IsSubmenu = 1)
--     |- Liquidaciones -> operacion del dia a dia, se queda en el sidebar
--   Mi perfil
--
-- El ORDEN de las filas importa: fk_menu_parent se valida fila a fila dentro
-- del mismo INSERT, asi que cada padre va antes que sus hijos.
INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder, IsSubmenu, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
('77771001-0000-0000-0000-000000000001', 'TENANT_DASHBOARD',   'Panel',          'activity',   '/tenant/dashboard',     NULL, 1, b'0', TRUE, TRUE, NULL, @now, @now),

('77771001-0000-0000-0000-000000000016', 'TENANT_AGENDA',      'Agenda',         'calendar-days', '/tenant/agenda',     NULL, 2, b'0', TRUE, TRUE, NULL, @now, @now),

('77771001-0000-0000-0000-000000000020', 'NEG',                'Negocio',        'briefcase',  NULL,                    NULL, 3, b'0', TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000030', 'TENANT_COMPANY',     'Mi empresa',     'building-2', '/tenant/mi-empresa',    '77771001-0000-0000-0000-000000000020', 2, b'0', TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000012', 'TENANT_SETTLEMENTS', 'Liquidaciones',  'banknote',   '/tenant/liquidaciones', '77771001-0000-0000-0000-000000000020', 3, b'0', TRUE, TRUE, NULL, @now, @now),
-- Nomina va SEPARADA de Liquidaciones porque son dos decisiones distintas:
-- liquidar aprueba trabajo y abona al saldo del colaborador; dispersar nomina
-- saca ese dinero de la caja y se lo consigna. Mezclarlas en una pantalla fue
-- justo lo que hacia que "liquidar" pareciera que ya se habia pagado.
-- Sus dos paneles son SUBMENUS: se pintan en el dock de la pantalla, no en el
-- carril lateral (mismo patron que "Mi empresa").
('77771001-0000-0000-0000-000000000013', 'TENANT_PAYROLL',   'Nómina',      'landmark', '/tenant/nomina',            '77771001-0000-0000-0000-000000000020', 4, b'0', TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000014', 'TENANT_PAYROLL_RUN','Dispersión', 'send',     '/tenant/nomina/dispersion', '77771001-0000-0000-0000-000000000013', 1, b'1', TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000015', 'TENANT_PAYROLL_LOG','Historial',  'history',  '/tenant/nomina/historial',  '77771001-0000-0000-0000-000000000013', 2, b'1', TRUE, TRUE, NULL, @now, @now),

-- Las seis configuraciones viven DENTRO de "Mi empresa". Sus rutas sueltas
-- siguen existiendo en el front (hay pasos de tour, enlaces guardados y specs
-- E2E que entran directo); lo que cambia es donde se dibujan. El orden es el
-- mismo recorrido que sigue quien configura.
('77771001-0000-0000-0000-000000000002', 'TENANT_BUSINESS',    'Mi negocio',     'building-2', '/tenant/mi-negocio',    '77770002-0000-0000-0000-000000000030', 1, b'1', TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000005', 'TENANT_SERVICES',    'Servicios',      'scissors',   '/tenant/servicios',     '77770002-0000-0000-0000-000000000030', 2, b'1', TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000006', 'TENANT_BRANCHES',    'Sedes',          'map-pin',    '/tenant/sedes',         '77770002-0000-0000-0000-000000000030', 3, b'1', TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000007', 'TENANT_EMPLOYEES',   'Empleados',      'users',      '/tenant/empleados',     '77770002-0000-0000-0000-000000000030', 4, b'1', TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000010', 'TENANT_PAGE',        'Mi página',      'globe',      '/tenant/mi-pagina',     '77770002-0000-0000-0000-000000000030', 5, b'1', TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000011', 'TENANT_FINANCE',     'Compensaciones', 'wallet',     '/tenant/compensaciones','77770002-0000-0000-0000-000000000030', 6, b'1', TRUE, TRUE, NULL, @now, @now),

('77771001-0000-0000-0000-000000000003', 'TENANT_PROFILE',     'Mi perfil',      'user',       '/tenant/profile',       NULL, 9, b'0', TRUE, TRUE, NULL, @now, @now),
-- "Crear mi negocio" no es una opcion de menu: el onboarding es un gate de
-- primera vez (la ruta redirige si el negocio ya existe). La fila se queda
-- apagada, no se borra, para no romper referencias a su Id.
('77771001-0000-0000-0000-000000000004', 'TENANT_ONBOARDING',  'Crear mi negocio', 'plus',     '/tenant/onboarding',    NULL, 8, b'0', FALSE, FALSE, NULL, @now, @now);

-- OWNER ve todo lo suyo. "Mi empresa" va aparte del LIKE: su Id quedo en la
-- familia 77770002 por como nacio (ver el filtro por Code de los menus ADMIN).
INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000004', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE (m.Id LIKE '77771001-0000-%' OR m.Code = 'TENANT_COMPANY')
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr
    WHERE mr.MenuId = m.Id
      AND mr.RoleId = '11111111-0000-0000-0000-000000000004'
);

-- EMPLOYEE: solo Panel y Mi perfil (opera desde el APK; el resto es del dueño).
INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000005', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE m.Id IN ('77771001-0000-0000-0000-000000000001', '77771001-0000-0000-0000-000000000003')
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr
    WHERE mr.MenuId = m.Id
      AND mr.RoleId = '11111111-0000-0000-0000-000000000005'
);

-- ---------------------------------------------------------------------
-- SEED: terceros de ejemplo
-- ---------------------------------------------------------------------
INSERT INTO personas.third_party
    (Id, DocumentTypeId, DocumentNumber, UserId,
     FirstName, SecondName, FirstLastName, SecondLastName,
     GenderId, BirthDate, PhotoUrl,
     Enabled, Visible, AuditUser, AuditDate, CreatedDate)
VALUES
    ('10000000-0000-0000-0000-000000000001', '55555555-0000-0000-0000-000000000001', '123456789', NULL,
     'Juan', 'Carlos', 'Perez', 'Rodriguez', NULL, NULL, NULL,
     TRUE, TRUE, NULL, @now, @now),
    ('10000000-0000-0000-0000-000000000002', '55555555-0000-0000-0000-000000000001', '987654321', NULL,
     'Maria', 'Fernanda', 'Gomez', 'Lopez', NULL, NULL, NULL,
     TRUE, TRUE, NULL, @now, @now);

-- =====================================================================
-- FASE B — catalogos, terceros (hijos), empresa y dominio de negocio.
-- Sin FK de BD a localizaciones (MunicipalityId/NeighborhoodId quedan
-- como columnas indexadas, validadas en la app). CreatedBy en todas.
-- =====================================================================
SET @now = NOW(6);

-- ----- Catalogos nuevos -----
CREATE TABLE shift_type (
    Id CHAR(36) NOT NULL, Code VARCHAR(80) NOT NULL, Name VARCHAR(120) NOT NULL, Value VARCHAR(500) NULL, DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), UNIQUE KEY uq_shift_type_code (Code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE schedule_type (
    Id CHAR(36) NOT NULL, Code VARCHAR(80) NOT NULL, Name VARCHAR(120) NOT NULL, Value VARCHAR(500) NULL, DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), UNIQUE KEY uq_schedule_type_code (Code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE employee_position (
    Id CHAR(36) NOT NULL, Code VARCHAR(80) NOT NULL, Name VARCHAR(120) NOT NULL, Value VARCHAR(500) NULL, DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), UNIQUE KEY uq_employee_position_code (Code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE address_type (
    Id CHAR(36) NOT NULL, Code VARCHAR(80) NOT NULL, Name VARCHAR(120) NOT NULL, Value VARCHAR(500) NULL, DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), UNIQUE KEY uq_address_type_code (Code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE day_of_week (
    Id CHAR(36) NOT NULL, Code VARCHAR(80) NOT NULL, Name VARCHAR(120) NOT NULL, Value VARCHAR(500) NULL, DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), UNIQUE KEY uq_day_of_week_code (Code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bancos. Catalogo normal (se administra desde Listas del sistema) y no una
-- lista fija en codigo: los bancos cambian, se fusionan y aparecen nuevos, y
-- ninguno de esos dias deberia requerir un despliegue.
CREATE TABLE bank (
    Id CHAR(36) NOT NULL, Code VARCHAR(80) NOT NULL, Name VARCHAR(120) NOT NULL, Value VARCHAR(500) NULL, DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), UNIQUE KEY uq_bank_code (Code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO bank (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
 ('67000000-0000-0000-0000-000000000001','BANCOLOMBIA','Bancolombia',NULL,1,TRUE,TRUE,NULL,@now,@now),
 ('67000000-0000-0000-0000-000000000002','DAVIVIENDA','Davivienda',NULL,2,TRUE,TRUE,NULL,@now,@now),
 ('67000000-0000-0000-0000-000000000003','BBVA','BBVA',NULL,3,TRUE,TRUE,NULL,@now,@now),
 ('67000000-0000-0000-0000-000000000004','BOGOTA','Banco de Bogotá',NULL,4,TRUE,TRUE,NULL,@now,@now),
 ('67000000-0000-0000-0000-000000000005','NEQUI','Nequi',NULL,5,TRUE,TRUE,NULL,@now,@now),
 ('67000000-0000-0000-0000-000000000006','DAVIPLATA','Daviplata',NULL,6,TRUE,TRUE,NULL,@now,@now),
 ('67000000-0000-0000-0000-000000000007','OCCIDENTE','Banco de Occidente',NULL,7,TRUE,TRUE,NULL,@now,@now),
 ('67000000-0000-0000-0000-000000000008','POPULAR','Banco Popular',NULL,8,TRUE,TRUE,NULL,@now,@now),
 ('67000000-0000-0000-0000-000000000009','COLPATRIA','Scotiabank Colpatria',NULL,9,TRUE,TRUE,NULL,@now,@now),
 ('67000000-0000-0000-0000-00000000000a','AGRARIO','Banco Agrario',NULL,10,TRUE,TRUE,NULL,@now,@now),
 ('67000000-0000-0000-0000-00000000000b','CAJA_SOCIAL','Banco Caja Social',NULL,11,TRUE,TRUE,NULL,@now,@now),
 ('67000000-0000-0000-0000-00000000000c','OTRO','Otro banco',NULL,99,TRUE,TRUE,NULL,@now,@now);

INSERT INTO system_list (Id, Code, Name, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
 ('44444444-0000-0000-0000-000000000009', 'bank', 'Bancos', 'Entidades a las que se les puede consignar la nómina.', TRUE, TRUE, NULL, @now, @now);

-- ----- Terceros: hijos -----
CREATE TABLE personas.third_party_contact (
    Id CHAR(36) NOT NULL, ThirdPartyId CHAR(36) NOT NULL, ContactTypeId CHAR(36) NOT NULL,
    Value VARCHAR(160) NOT NULL, IsPrimary BOOLEAN NOT NULL DEFAULT FALSE, IsVerified BOOLEAN NOT NULL DEFAULT FALSE, VerifiedAt DATETIME(6) NULL, Notes VARCHAR(255) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_tpc_third_party (ThirdPartyId), KEY idx_tpc_contact_type (ContactTypeId),
    CONSTRAINT fk_tpc_third_party FOREIGN KEY (ThirdPartyId) REFERENCES personas.third_party (Id)
    -- Sin FK: los catalogos viven en saas_db y los gobierna
    -- system-service. Es la frontera entre esquemas, no un descuido.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Cuentas bancarias de una persona: a donde se le consigna la nomina.
--
-- Existe porque el dia de pago el dueño necesita el dato A LA MANO. Sin esto
-- tenia que perseguir a cada empleado por WhatsApp justo cuando esta pagando,
-- que es el peor momento posible.
--
-- Dos formas, condicionadas por AccountKind:
--   BANK  -> BankId + AccountType (ahorros/corriente) + AccountNumber
--   BREV  -> BrevKey TAL CUAL la escribio la persona (puede ser su celular,
--            su correo, su documento o un alfanumerico). NO se normaliza: si
--            se le quita un punto o se le cambia una mayuscula, la
--            transferencia se va a otra parte o rebota.
--
-- Una sola principal por persona, garantizado por BD y no por la aplicacion:
-- PrimaryOwner vale el ThirdPartyId cuando la cuenta es principal y NULL
-- cuando no, y el UNIQUE sobre esa columna hace imposible tener dos. En MySQL
-- los NULL no chocan entre si, asi que las demas cuentas conviven sin
-- estorbarse.
-- ---------------------------------------------------------------------
CREATE TABLE personas.bank_account (
    Id            CHAR(36)    NOT NULL,
    ThirdPartyId  CHAR(36)    NOT NULL,
    AccountKind   VARCHAR(16) NOT NULL,
    BankId        CHAR(36)    NULL,
    AccountType   VARCHAR(16) NULL,
    AccountNumber VARCHAR(40) NULL,
    BrevKey       VARCHAR(120) NULL,
    -- Como la reconoce su dueño ("la de la nomina"). Opcional.
    Alias         VARCHAR(60) NULL,
    IsPrimary     BOOLEAN     NOT NULL DEFAULT FALSE,
    PrimaryOwner  CHAR(36) GENERATED ALWAYS AS (IF(IsPrimary, ThirdPartyId, NULL)) STORED,
    Enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN     NOT NULL DEFAULT TRUE,
    CreatedBy   CHAR(36)    NULL,
    AuditUser   CHAR(36)    NULL,
    AuditDate   DATETIME(6) NOT NULL,
    CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    KEY idx_ba_owner (ThirdPartyId),
    UNIQUE KEY uq_ba_one_primary (PrimaryOwner),
    CONSTRAINT fk_ba_thirdparty FOREIGN KEY (ThirdPartyId) REFERENCES personas.third_party (Id)
    -- Sin FK: los catalogos viven en saas_db y los gobierna
    -- system-service. Es la frontera entre esquemas, no un descuido.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE personas.third_party_address (
    Id CHAR(36) NOT NULL, ThirdPartyId CHAR(36) NOT NULL, AddressTypeId CHAR(36) NULL,
    MunicipalityId CHAR(36) NOT NULL, NeighborhoodId CHAR(36) NULL, Line VARCHAR(255) NULL, Reference VARCHAR(255) NULL, IsPrimary BOOLEAN NOT NULL DEFAULT FALSE,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_tpa_third_party (ThirdPartyId), KEY idx_tpa_municipality (MunicipalityId), KEY idx_tpa_neighborhood (NeighborhoodId),
    CONSTRAINT fk_tpa_third_party FOREIGN KEY (ThirdPartyId) REFERENCES personas.third_party (Id)
    -- Sin FK: los catalogos viven en saas_db y los gobierna
    -- system-service. Es la frontera entre esquemas, no un descuido.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- OUTBOX DE `personas`
-- ---------------------------------------------------------------------
-- Si, es la misma tabla que en saas_db, y NO es duplicacion: el patron
-- exige que la fila del evento se escriba en la MISMA TRANSACCION que el
-- cambio de dominio, y una transaccion no cruza bases de datos. Un
-- outbox compartido en saas_db haria que thirdparty-service escribiera
-- la persona en un sitio y el evento en otro, sin atomicidad entre los
-- dos: exactamente el problema que el outbox existe para evitar.
--
-- Cada esquema que PUBLICA eventos tiene el suyo. saas_events no tiene
-- porque solo consume.
-- =====================================================================
CREATE TABLE personas.outbox_event (
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
       UNIQUE KEY uk_personas_outbox_event_id (EventId),

    -- Indice para que el relay haga "WHERE Status='PENDING' ORDER BY CreatedAt"
    -- de manera eficiente. Sin indice seria full-scan (lento con miles de rows).
                              INDEX idx_personas_outbox_status_created (Status, CreatedAt)
);

-- ----- Empresa y derivados -----
CREATE TABLE business (
    Id CHAR(36) NOT NULL, BusinessTypeId CHAR(36) NOT NULL, Name VARCHAR(160) NOT NULL, LegalName VARCHAR(200) NULL, TradeName VARCHAR(160) NULL,
    DocumentTypeId CHAR(36) NULL, DocumentNumber VARCHAR(40) NULL, LogoUrl VARCHAR(500) NULL, StatusId CHAR(36) NULL,
    PrimaryColor VARCHAR(20) NULL, SecondaryColor VARCHAR(20) NULL,
    -- Zona horaria del negocio. La agenda guarda SIEMPRE UTC y usa esto
    -- para presentarla. Va aqui y no en la politica de reservas porque
    -- tambien la necesitan los cortes de nomina y los informes.
    TimeZone VARCHAR(64) NOT NULL DEFAULT 'America/Bogota',
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    CONSTRAINT fk_business_type FOREIGN KEY (BusinessTypeId) REFERENCES business_type (Id),
    CONSTRAINT fk_business_doc_type FOREIGN KEY (DocumentTypeId) REFERENCES document_type (Id),
    CONSTRAINT fk_business_status FOREIGN KEY (StatusId) REFERENCES status (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Pagina publica del negocio (1:1 con business), accedida por subdominio/slug
-- y editable por el dueno con vista previa. GalleryJson es un arreglo JSON de
-- URLs (["url1","url2",...]).
CREATE TABLE business_landing (
    Id           CHAR(36)     NOT NULL,
    BusinessId   CHAR(36)     NOT NULL,
    Tagline      VARCHAR(160) NULL,
    About        TEXT         NULL,
    Phone        VARCHAR(40)  NULL,
    Whatsapp     VARCHAR(40)  NULL,
    ContactEmail VARCHAR(120) NULL,
    Instagram    VARCHAR(160) NULL,
    Facebook     VARCHAR(160) NULL,
    HeroImageUrl VARCHAR(500) NULL,
    GalleryJson  JSON         NULL,
    ScheduleText VARCHAR(400) NULL,
    Published    BOOLEAN      NOT NULL DEFAULT FALSE,
    Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy    CHAR(36)     NULL,
    AuditUser    CHAR(36)     NULL,
    AuditDate    DATETIME(6)  NOT NULL,
    CreatedDate  DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_business_landing_business (BusinessId),
    CONSTRAINT fk_business_landing_business FOREIGN KEY (BusinessId) REFERENCES business (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dominios/slug por empresa: una empresa puede tener varios (uno primario).
-- Separado de business: el slug y la gestion de dominios tienen su propio ciclo
-- de vida (verificacion, dominio propio) y no deben mezclarse con la identidad
-- de la empresa.
CREATE TABLE business_domain (
    Id CHAR(36) NOT NULL, BusinessId CHAR(36) NOT NULL, Slug VARCHAR(63) NOT NULL, CustomDomain VARCHAR(255) NULL,
    IsPrimary BOOLEAN NOT NULL DEFAULT FALSE, IsVerified BOOLEAN NOT NULL DEFAULT FALSE, VerifiedDate DATETIME(6) NULL, StatusId CHAR(36) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_business_domain_slug (Slug),
    UNIQUE KEY uq_business_domain_custom (CustomDomain),
    KEY idx_business_domain_business (BusinessId),
    CONSTRAINT fk_business_domain_business FOREIGN KEY (BusinessId) REFERENCES business (Id),
    CONSTRAINT fk_business_domain_status FOREIGN KEY (StatusId) REFERENCES status (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE branch (
    Id CHAR(36) NOT NULL, BusinessId CHAR(36) NOT NULL, BranchTypeId CHAR(36) NOT NULL, Name VARCHAR(160) NOT NULL, Code VARCHAR(40) NULL,
    MunicipalityId CHAR(36) NOT NULL, NeighborhoodId CHAR(36) NULL, AddressLine VARCHAR(255) NULL,
    -- Donde esta en el mapa. Va aqui y no en la ficha del directorio
    -- porque un punto del mapa es un LOCAL: un negocio con tres sedes
    -- tiene tres sitios. NULL = no lo ha marcado, y entonces sale en
    -- el listado pero no en el mapa (mejor que un pin inventado).
    Latitude DECIMAL(10,7) NULL, Longitude DECIMAL(10,7) NULL,
    Phone VARCHAR(30) NULL, IsMain BOOLEAN NOT NULL DEFAULT FALSE, StatusId CHAR(36) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_branch_business (BusinessId), KEY idx_branch_municipality (MunicipalityId),
    CONSTRAINT fk_branch_business FOREIGN KEY (BusinessId) REFERENCES business (Id),
    CONSTRAINT fk_branch_type FOREIGN KEY (BranchTypeId) REFERENCES branch_type (Id),
    CONSTRAINT fk_branch_status FOREIGN KEY (StatusId) REFERENCES status (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_owner (
    Id CHAR(36) NOT NULL, BusinessId CHAR(36) NOT NULL, ThirdPartyId CHAR(36) NOT NULL, OwnershipPercentage DECIMAL(5,2) NOT NULL, StartDate DATE NOT NULL, EndDate DATE NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_bo_business (BusinessId), KEY idx_bo_third_party (ThirdPartyId),
    CONSTRAINT fk_bo_business FOREIGN KEY (BusinessId) REFERENCES business (Id)
    -- ThirdPartyId es una REFERENCIA al esquema `personas`, no una FK
    -- dura: son dos esquemas de dos servicios distintos.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- PositionId/HireDate NULL: el alta minima crea al empleado con sus FKs y el
-- resto llega cuando completa su perfil. SpecialtyId es nullable y SIN FK dura
-- (igual que business_offering.CategoryId): no acopla el orden de inserciones.
CREATE TABLE employee (
    Id CHAR(36) NOT NULL, ThirdPartyId CHAR(36) NOT NULL, BranchId CHAR(36) NOT NULL, PositionId CHAR(36) NULL, EmployeeCode VARCHAR(40) NULL, HireDate DATE NULL, TerminationDate DATE NULL, StatusId CHAR(36) NULL, SpecialtyId CHAR(36) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_emp_branch (BranchId), KEY idx_emp_third_party (ThirdPartyId),
    -- ThirdPartyId es una REFERENCIA al esquema `personas`, no una FK
    -- dura: son dos esquemas de dos servicios distintos.
    CONSTRAINT fk_emp_branch FOREIGN KEY (BranchId) REFERENCES branch (Id),
    CONSTRAINT fk_emp_position FOREIGN KEY (PositionId) REFERENCES employee_position (Id),
    CONSTRAINT fk_emp_status FOREIGN KEY (StatusId) REFERENCES status (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- EL CLIENTE DE UN NEGOCIO
-- ---------------------------------------------------------------------
-- Sustituye a la antigua tabla `client`, que era un cliente GLOBAL sin
-- vinculo con ningun negocio: existia, tenia controlador, y nadie la
-- usaba. Los datos del cliente vivian sueltos dentro de service_charge.
--
-- La persona (quien es) vive en `personas.third_party`. Esta tabla dice
-- que esa persona es cliente DE ESTE negocio, y es lo unico que un
-- negocio puede consultar. Asi la regla de privacidad no depende de que
-- alguien se acuerde de filtrar: para llegar a una persona hay que pasar
-- por aqui, y aqui ya esta el BusinessId.
--
-- ThirdPartyId es NULL a proposito: es el cliente de mostrador, el que
-- llega sin cita y sin telefono. Tiene nombre para el recibo y nada mas;
-- no recibe mensajes ni puede consultar por codigo publico. Darle un
-- telefono inventado para "que cuadre" seria mucho peor: ensuciaria la
-- identidad por telefono de la que depende todo lo demas.
-- =====================================================================
CREATE TABLE business_client (
    Id           CHAR(36)     NOT NULL,
    BusinessId   CHAR(36)     NOT NULL,
    -- Referencia al esquema `personas`. NULL = cliente de mostrador.
    ThirdPartyId CHAR(36)     NULL,
    -- Como se le llama en la agenda. Para el de mostrador es lo unico que hay.
    DisplayName  VARCHAR(160) NOT NULL,
    -- E.164 SIEMPRE (+573001234567). Es la identidad del cliente: se
    -- normaliza en un solo punto de la aplicacion, nunca en cada pantalla.
    PhoneE164    VARCHAR(20)  NULL,
    PhoneVerifiedAt DATETIME(6) NULL,

    -- Consentimiento de WhatsApp. Se guarda CUANDO y QUE TEXTO acepto: sin
    -- las dos cosas no hay forma de demostrar el opt-in si alguien reclama.
    WhatsappOptInAt   DATETIME(6)  NULL,
    WhatsappOptInText VARCHAR(500) NULL,
    -- La baja se respeta de verdad: con fecha aqui, no se le escribe mas.
    WhatsappOptOutAt  DATETIME(6)  NULL,

    AcquisitionSource VARCHAR(80)  NULL,
    Notes             VARCHAR(500) NULL,
    -- Contadores de comportamiento. Alimentan la politica de bloqueo por
    -- inasistencia sin tener que recorrer la agenda entera cada vez.
    NoShowCount  INT NOT NULL DEFAULT 0,
    VisitCount   INT NOT NULL DEFAULT 0,
    LastVisitAt  DATETIME(6) NULL,

    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    -- Un telefono es UN cliente dentro de un negocio. Sin esto, cada
    -- reserva por WhatsApp crearia una ficha nueva y el historial del
    -- cliente se partiria en trozos.
    UNIQUE KEY uq_bc_business_phone (BusinessId, PhoneE164),
    KEY idx_bc_business (BusinessId),
    KEY idx_bc_third_party (ThirdPartyId),
    CONSTRAINT fk_bc_business FOREIGN KEY (BusinessId) REFERENCES business (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----- Ofertas (servicios que ofrece la empresa) -----
-- Catalogo de ESPECIALIDADES per-business (espejo de offering_category): agrupa
-- servicios por disciplina (Barberia, Estilismo, Manicure...) y clasifica al
-- empleado. Habilita simular la compensacion por % de servicio filtrando a la
-- especialidad del empleado.
CREATE TABLE specialty (
    Id CHAR(36) NOT NULL, BusinessId CHAR(36) NOT NULL, Name VARCHAR(120) NOT NULL, DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_specialty_business (BusinessId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE offering_category (
    Id CHAR(36) NOT NULL, BusinessId CHAR(36) NOT NULL, Name VARCHAR(120) NOT NULL, DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_oc_business (BusinessId),
    CONSTRAINT fk_oc_business FOREIGN KEY (BusinessId) REFERENCES business (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_offering (
    Id CHAR(36) NOT NULL, BusinessId CHAR(36) NOT NULL, CategoryId CHAR(36) NULL, SpecialtyId CHAR(36) NULL, Name VARCHAR(160) NOT NULL, Description VARCHAR(500) NULL, DurationMinutes INT NOT NULL, Price DECIMAL(12,2) NOT NULL, IsActive BOOLEAN NOT NULL DEFAULT TRUE,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_boff_business (BusinessId),
    CONSTRAINT fk_boff_business FOREIGN KEY (BusinessId) REFERENCES business (Id),
    CONSTRAINT fk_boff_category FOREIGN KEY (CategoryId) REFERENCES offering_category (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE branch_offering (
    Id CHAR(36) NOT NULL, BranchId CHAR(36) NOT NULL, OfferingId CHAR(36) NULL, Name VARCHAR(160) NULL, Description VARCHAR(500) NULL, DurationMinutes INT NULL, Price DECIMAL(12,2) NULL, IsEnabled BOOLEAN NOT NULL DEFAULT TRUE, IsActive BOOLEAN NOT NULL DEFAULT TRUE,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_broff_branch (BranchId),
    CONSTRAINT fk_broff_branch FOREIGN KEY (BranchId) REFERENCES branch (Id),
    CONSTRAINT fk_broff_offering FOREIGN KEY (OfferingId) REFERENCES business_offering (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----- Horarios -----
CREATE TABLE business_schedule (
    Id CHAR(36) NOT NULL, BusinessId CHAR(36) NOT NULL, ScheduleTypeId CHAR(36) NOT NULL, Name VARCHAR(120) NOT NULL, ValidFrom DATETIME(6) NOT NULL, ValidTo DATETIME(6) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_bsch_business (BusinessId),
    CONSTRAINT fk_bsch_business FOREIGN KEY (BusinessId) REFERENCES business (Id),
    CONSTRAINT fk_bsch_type FOREIGN KEY (ScheduleTypeId) REFERENCES schedule_type (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_schedule_shift (
    Id CHAR(36) NOT NULL, BusinessScheduleId CHAR(36) NOT NULL, ShiftTypeId CHAR(36) NOT NULL, DayOfWeekId CHAR(36) NOT NULL, StartTime TIME NOT NULL, EndTime TIME NOT NULL, DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_bschs_schedule (BusinessScheduleId),
    CONSTRAINT fk_bschs_schedule FOREIGN KEY (BusinessScheduleId) REFERENCES business_schedule (Id),
    CONSTRAINT fk_bschs_shift_type FOREIGN KEY (ShiftTypeId) REFERENCES shift_type (Id),
    CONSTRAINT fk_bschs_day FOREIGN KEY (DayOfWeekId) REFERENCES day_of_week (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE branch_schedule (
    Id CHAR(36) NOT NULL, BranchId CHAR(36) NOT NULL, BusinessScheduleId CHAR(36) NULL, ScheduleTypeId CHAR(36) NOT NULL, Name VARCHAR(120) NOT NULL, ValidFrom DATETIME(6) NOT NULL, ValidTo DATETIME(6) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_brsch_branch (BranchId),
    CONSTRAINT fk_brsch_branch FOREIGN KEY (BranchId) REFERENCES branch (Id),
    CONSTRAINT fk_brsch_business_schedule FOREIGN KEY (BusinessScheduleId) REFERENCES business_schedule (Id),
    CONSTRAINT fk_brsch_type FOREIGN KEY (ScheduleTypeId) REFERENCES schedule_type (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE branch_schedule_shift (
    Id CHAR(36) NOT NULL, BranchScheduleId CHAR(36) NOT NULL, ShiftTypeId CHAR(36) NOT NULL, DayOfWeekId CHAR(36) NOT NULL, StartTime TIME NOT NULL, EndTime TIME NOT NULL, DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_brschs_schedule (BranchScheduleId),
    CONSTRAINT fk_brschs_schedule FOREIGN KEY (BranchScheduleId) REFERENCES branch_schedule (Id),
    CONSTRAINT fk_brschs_shift_type FOREIGN KEY (ShiftTypeId) REFERENCES shift_type (Id),
    CONSTRAINT fk_brschs_day FOREIGN KEY (DayOfWeekId) REFERENCES day_of_week (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE employee_shift_assignment (
    Id CHAR(36) NOT NULL, EmployeeId CHAR(36) NOT NULL, BranchScheduleShiftId CHAR(36) NOT NULL, IsFullShift BOOLEAN NOT NULL DEFAULT TRUE, CustomStartTime TIME NULL, CustomEndTime TIME NULL, StatusId CHAR(36) NULL, ValidFrom DATETIME(6) NOT NULL, ValidTo DATETIME(6) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_esa_employee (EmployeeId),
    CONSTRAINT fk_esa_employee FOREIGN KEY (EmployeeId) REFERENCES employee (Id),
    CONSTRAINT fk_esa_shift FOREIGN KEY (BranchScheduleShiftId) REFERENCES branch_schedule_shift (Id),
    CONSTRAINT fk_esa_status FOREIGN KEY (StatusId) REFERENCES status (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Compensacion jerarquica en 3 niveles: business (general) -> branch (sede)
-- -> employee (individual). Cada nivel guarda su propia configuracion
-- versionada (ValidFrom/ValidTo; vigente = ValidTo IS NULL). La resolucion
-- efectiva escala de abajo hacia arriba: si el empleado no tiene config
-- vigente toma la de su sede, y si la sede no tiene, la del negocio.
-- Misma forma en los 3: (Type, Value) condicionado por Type.
--
-- SalaryBase existe solo para los tipos HIBRIDOS ("Salario + % servicio",
-- "Salario + comision"): ahi CompensationValue guarda el % y SalaryBase el
-- salario. Los tipos no-hibridos lo dejan NULL y CompensationValue mantiene
-- su significado de siempre.
-- ---------------------------------------------------------------------
-- PayrollFrequency (MONTHLY|BIWEEKLY|WEEKLY) vive AQUI, a nivel de negocio: es
-- cada cuanto la empresa dispersa nomina, no como se le paga a una persona.
-- De el sale en cuantas partes se fracciona el sueldo base al abonarse al
-- saldo, y por tanto la clave de periodo que evita abonarlo dos veces.
CREATE TABLE business_compensation (
    Id CHAR(36) NOT NULL, BusinessId CHAR(36) NOT NULL, CompensationType VARCHAR(40) NOT NULL, CompensationValue DECIMAL(12,2) NOT NULL, SalaryBase DECIMAL(12,2) NULL, ValidFrom DATETIME(6) NOT NULL, ValidTo DATETIME(6) NULL,
    PayrollFrequency VARCHAR(16) NOT NULL DEFAULT 'MONTHLY',
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_bizcomp_business (BusinessId),
    CONSTRAINT fk_bizcomp_business FOREIGN KEY (BusinessId) REFERENCES business (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE branch_compensation (
    Id CHAR(36) NOT NULL, BranchId CHAR(36) NOT NULL, CompensationType VARCHAR(40) NOT NULL, CompensationValue DECIMAL(12,2) NOT NULL, SalaryBase DECIMAL(12,2) NULL, ValidFrom DATETIME(6) NOT NULL, ValidTo DATETIME(6) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_brcomp_branch (BranchId),
    CONSTRAINT fk_brcomp_branch FOREIGN KEY (BranchId) REFERENCES branch (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE employee_compensation (
    Id CHAR(36) NOT NULL, EmployeeId CHAR(36) NOT NULL, CompensationType VARCHAR(40) NOT NULL, CompensationValue DECIMAL(12,2) NOT NULL, SalaryBase DECIMAL(12,2) NULL, ValidFrom DATETIME(6) NOT NULL, ValidTo DATETIME(6) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_ec_employee (EmployeeId),
    CONSTRAINT fk_ec_employee FOREIGN KEY (EmployeeId) REFERENCES employee (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Saldo del empleado (read model materializado).
--
-- El saldo es lo primero que el empleado ve en el APK. En vez de recalcularlo
-- al vuelo en cada consulta (servicios prestados x compensacion - pagos), se
-- MATERIALIZA aqui y se refresca por evento cuando cambia una de sus entradas.
-- Ademas se proyecta a Elasticsearch (indice employee_balances).
--
-- AmountAccrued/AmountPaid arrancan en 0: todavia no existe el modulo de
-- servicios prestados/pagos que los alimente. La tuberia queda lista.
-- ---------------------------------------------------------------------
CREATE TABLE employee_balance (
    Id CHAR(36) NOT NULL,
    BusinessId CHAR(36) NOT NULL,
    BranchId CHAR(36) NULL,
    EmployeeId CHAR(36) NOT NULL,
    ThirdPartyId CHAR(36) NULL,
    UserId CHAR(36) NULL,
    -- Devengado por servicios prestados (futuro modulo de agenda/servicios).
    AmountAccrued DECIMAL(14,2) NOT NULL DEFAULT 0,
    -- Pagado al empleado (futuro modulo de pagos).
    AmountPaid DECIMAL(14,2) NOT NULL DEFAULT 0,
    -- Por cobrar = AmountAccrued - AmountPaid. Denormalizado para leer directo.
    Balance DECIMAL(14,2) NOT NULL DEFAULT 0,
    Currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    LastCalculatedAt DATETIME(6) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    -- EmployeeId es una REFERENCIA al agregado de business-service, NO una FK
    -- dura: el saldo se inicializa (S2S) durante el aprovisionamiento, antes de
    -- que la transaccion de business confirme la fila de employee. Una FK
    -- cross-servicio la rechazaria. Se indexa como clave normal.
    UNIQUE KEY uq_eb_employee (EmployeeId),
    KEY idx_eb_business (BusinessId),
    KEY idx_eb_user (UserId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Dispersion de nomina: el lote con el que la empresa PAGA de verdad.
--
-- Liquidar y pagar son dos cosas distintas y hasta ahora estaban mezcladas.
-- Liquidar aprueba servicios y ABONA al saldo del empleado (el empleado gana);
-- dispersar nomina SACA ese saldo de la caja de la empresa y se lo consigna
-- (el empleado cobra). Esta tabla es la segunda mitad: una fila por corrida.
--
-- No hay Id de transaccion bancaria: el sistema no habla con el banco. Lo que
-- se guarda es la constancia interna (Code) y, si el dueño la anota, la cuenta
-- destino de cada empleado (en su movimiento).
--
-- Status: COMPLETED cuando todos los empleados del lote se pagaron; PARTIAL si
-- alguno quedo fuera (p. ej. su saldo cambio entre que se listo y se ejecuto).
-- No existe "en proceso" persistido: la corrida se resuelve dentro de una sola
-- transaccion, y una fila colgada en PROCESSING seria mentira.
-- ---------------------------------------------------------------------
CREATE TABLE payroll_run (
    Id CHAR(36) NOT NULL,
    BusinessId CHAR(36) NOT NULL,
    BranchId CHAR(36) NULL,
    -- Constancia legible para el dueño y para el correo (DP-AAAAMMDD-XXXX).
    Code VARCHAR(32) NOT NULL,
    PeriodLabel VARCHAR(60) NOT NULL,
    PeriodStart DATE NOT NULL,
    PeriodEnd DATE NOT NULL,
    EmployeeCount INT NOT NULL DEFAULT 0,
    TotalAmount DECIMAL(14,2) NOT NULL DEFAULT 0,
    Currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    Status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    ExecutedAt DATETIME(6) NOT NULL,
    -- Anular NO borra: la fila sigue diciendo que la plata salio ese dia,
    -- porque eso paso. Lo que cambia es el estado y lo que se escribe encima
    -- (un PAYROLL_REVERSAL por pago). El motivo es obligatorio al anular: una
    -- anulacion de dinero sin explicacion no se puede auditar.
    VoidedAt DATETIME(6) NULL,
    VoidReason VARCHAR(300) NULL,
    Note VARCHAR(255) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    -- Clave que manda la pantalla al enviar. Dos envios del mismo formulario
    -- traen la misma clave, chocan contra el indice unico y el segundo recibe
    -- la corrida que ya existe en vez de pagar por segunda vez. Sin esto, un
    -- doble clic dispersa la nomina dos veces.
    IdempotencyKey VARCHAR(64) NULL,

    PRIMARY KEY (Id),
    UNIQUE KEY uq_pr_code (Code),
    -- Nulable a proposito: las corridas de antes de que existiera la clave no
    -- tienen ninguna, y en MySQL varios NULL conviven en un indice unico.
    UNIQUE KEY uq_pr_idempotency (BusinessId, IdempotencyKey),
    KEY idx_pr_business_date (BusinessId, ExecutedAt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Movimientos del saldo del empleado (auditoria de tesoreria).
--
-- Una sola tabla para las dos direcciones del dinero, porque para el empleado
-- son la misma lista: su extracto. MovementType dice hacia donde va:
--
--   COMMISSION  (+) liquidacion de servicios aprobados -> sube AmountAccrued
--   BASE_SALARY (+) abono programado del sueldo base   -> sube AmountAccrued
--   PAYROLL     (-) dispersion de nomina               -> sube AmountPaid
--
-- Partirlo en dos tablas obligaria a unir y ordenar por fecha en cada lectura
-- del extracto, que es justo la consulta que mas se hace.
--
-- PeriodKey solo lo usa BASE_SALARY ('2026-08', '2026-08-Q1'): es la garantia
-- de que el sueldo base de un periodo se abona UNA vez. El unique lo cubre; en
-- MySQL varios NULL conviven en un indice unico, asi que los otros dos tipos
-- (que lo dejan NULL) no se estorban entre si.
--
-- CommissionAmount/BaseSalaryAmount desglosan un PAYROLL: sin ese desglose
-- congelado, el empleado ve un unico numero y no sabe cuanto fue comision y
-- cuanto sueldo. Se guardan en vez de recalcularse por la misma razon que
-- BalanceBefore: la compensacion es versionada y el pasado no se recalcula.
-- ---------------------------------------------------------------------
CREATE TABLE employee_settlement (
    Id CHAR(36) NOT NULL,
    BusinessId CHAR(36) NOT NULL,
    BranchId CHAR(36) NULL,
    EmployeeId CHAR(36) NOT NULL,
    -- Monto del movimiento, SIEMPRE positivo. El signo lo pone MovementType.
    Amount DECIMAL(14,2) NOT NULL,
    -- Saldo por cobrar que tenia el empleado justo antes. Deja la foto del
    -- momento para poder auditar sin recalcular historia.
    BalanceBefore DECIMAL(14,2) NOT NULL,
    Currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    SettledAt DATETIME(6) NOT NULL,
    Note VARCHAR(255) NULL,

    MovementType VARCHAR(20) NOT NULL DEFAULT 'COMMISSION',
    PeriodKey VARCHAR(16) NULL,
    PayrollRunId CHAR(36) NULL,
    -- El movimiento que este deshace (solo en PAYROLL_REVERSAL). Con esto el
    -- extracto se lee entero: "se te pago X" y debajo "se anulo X".
    ReversalOfId CHAR(36) NULL,
    -- Desglose de un PAYROLL (0 en los movimientos de abono).
    CommissionAmount DECIMAL(14,2) NOT NULL DEFAULT 0,
    BaseSalaryAmount DECIMAL(14,2) NOT NULL DEFAULT 0,
    -- Retrato EN TEXTO de la cuenta a la que se consigno ("Bancolombia ·
    -- Ahorros ···4590"). Se congela porque la cuenta se puede editar o borrar
    -- despues, y un comprobante que cambia solo no es un comprobante.
    PayoutAccount VARCHAR(120) NULL,
    -- Referencia a la cuenta usada. Sin clave foranea dura: bank_account la
    -- gobierna thirdparty-service, igual que EmployeeId.
    BankAccountId CHAR(36) NULL,

    -- PRUEBA DEL PAGO. Un pago de nomina no se registra sin una de las dos:
    --   PaymentProofUrl -> la foto del comprobante de la transferencia
    --   PaidInCash      -> el dueño declara que pago en mano
    -- Sin esto, "ya te pague" era la palabra del dueño contra la del empleado.
    PaymentProofUrl VARCHAR(500) NULL,
    -- SHA-256 del fichero, guardado al subirlo. No se rechaza el repetido: un
    -- negocio que paga a cinco personas en UNA transferencia tiene un solo
    -- soporte para los cinco, y eso es correcto. Falta era que se NOTARA, y
    -- eso se cuenta al leer — un indicador guardado se queda viejo en cuanto
    -- se anula un pago.
    PaymentProofHash CHAR(64) NULL,
    PaidInCash BOOLEAN NOT NULL DEFAULT FALSE,
    -- El acuse del empleado en el movil, SOLO para los pagos en efectivo: en una
    -- transferencia el comprobante ya lo demuestra, en efectivo no hay rastro
    -- salvo que la persona diga "si, lo recibi".
    CashConfirmedAt DATETIME(6) NULL,

    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    -- Mismas razones que employee_balance: EmployeeId es referencia
    -- cross-servicio, no FK dura. Se indexa para el historial.
    KEY idx_es_employee (EmployeeId),
    KEY idx_es_business (BusinessId),
    KEY idx_es_settled (SettledAt),
    KEY idx_es_run (PayrollRunId),
    -- Los pagos en efectivo sin acusar: es la consulta que alimenta el aviso
    -- del movil y el pendiente que ve el dueño.
    KEY idx_es_cash_pending (EmployeeId, PaidInCash, CashConfirmedAt),
    UNIQUE KEY uq_es_period (EmployeeId, MovementType, PeriodKey),
    KEY idx_es_proof_hash (BusinessId, PaymentProofHash),
    -- Un movimiento se deshace UNA vez. Sin esta clave, anular dos veces la
    -- misma corrida devolveria el saldo dos veces, y la segunda es dinero
    -- inventado.
    UNIQUE KEY uq_es_reversal (ReversalOfId),
    CONSTRAINT fk_es_run FOREIGN KEY (PayrollRunId) REFERENCES payroll_run (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----- Seeds de catalogos -----
INSERT INTO shift_type (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
 ('60000000-0000-0000-0000-000000000001','MORNING','Manana','MORNING',1,TRUE,TRUE,NULL,@now,@now),
 ('60000000-0000-0000-0000-000000000002','AFTERNOON','Tarde','AFTERNOON',2,TRUE,TRUE,NULL,@now,@now),
 ('60000000-0000-0000-0000-000000000003','NIGHT','Noche','NIGHT',3,TRUE,TRUE,NULL,@now,@now);
INSERT INTO schedule_type (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
 ('61000000-0000-0000-0000-000000000001','CONTINUOUS','Continuo','CONTINUOUS',1,TRUE,TRUE,NULL,@now,@now),
 ('61000000-0000-0000-0000-000000000002','DISCONTINUOUS','Discontinuo','DISCONTINUOUS',2,TRUE,TRUE,NULL,@now,@now);
-- Tipos de negocio y de sede: sin items el dueño no puede crear su negocio
-- ni sus sedes (selects vacios en onboarding/sedes).
INSERT INTO business_type (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
 ('65000000-0000-0000-0000-000000000001','BARBERSHOP','Barberia',NULL,1,TRUE,TRUE,NULL,@now,@now),
 ('65000000-0000-0000-0000-000000000002','SALON','Salon de belleza',NULL,2,TRUE,TRUE,NULL,@now,@now),
 ('65000000-0000-0000-0000-000000000003','SPA','Spa',NULL,3,TRUE,TRUE,NULL,@now,@now),
 ('65000000-0000-0000-0000-000000000004','NAILS','Estudio de unas',NULL,4,TRUE,TRUE,NULL,@now,@now);

INSERT INTO branch_type (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
 ('66000000-0000-0000-0000-000000000001','MAIN','Principal',NULL,1,TRUE,TRUE,NULL,@now,@now),
 ('66000000-0000-0000-0000-000000000002','BRANCH','Sucursal',NULL,2,TRUE,TRUE,NULL,@now,@now),
 ('66000000-0000-0000-0000-000000000003','KIOSK','Punto satelite',NULL,3,TRUE,TRUE,NULL,@now,@now);

INSERT INTO employee_position (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
 ('62000000-0000-0000-0000-000000000001','BARBER','Barbero','BARBER',1,TRUE,TRUE,NULL,@now,@now),
 ('62000000-0000-0000-0000-000000000002','STYLIST','Estilista','STYLIST',2,TRUE,TRUE,NULL,@now,@now),
 ('62000000-0000-0000-0000-000000000003','MANICURIST','Manicurista','MANICURIST',3,TRUE,TRUE,NULL,@now,@now),
 ('62000000-0000-0000-0000-000000000004','RECEPTIONIST','Recepcionista','RECEPTIONIST',4,TRUE,TRUE,NULL,@now,@now);
INSERT INTO address_type (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
 ('63000000-0000-0000-0000-000000000001','HOME','Casa','HOME',1,TRUE,TRUE,NULL,@now,@now),
 ('63000000-0000-0000-0000-000000000002','WORK','Trabajo','WORK',2,TRUE,TRUE,NULL,@now,@now),
 ('63000000-0000-0000-0000-000000000003','BILLING','Facturacion','BILLING',3,TRUE,TRUE,NULL,@now,@now);

-- Tipos de contacto (medios de un tercero). Sin estos, el dropdown de
-- contactos del tercero saldría vacío.
INSERT INTO contact_type (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
 ('62000000-0000-0000-0000-000000000001','MOBILE','Celular','MOBILE',1,TRUE,TRUE,NULL,@now,@now),
 ('62000000-0000-0000-0000-000000000002','EMAIL','Correo','EMAIL',2,TRUE,TRUE,NULL,@now,@now),
 ('62000000-0000-0000-0000-000000000003','PHONE','Teléfono fijo','PHONE',3,TRUE,TRUE,NULL,@now,@now),
 ('62000000-0000-0000-0000-000000000004','WHATSAPP','WhatsApp','WHATSAPP',4,TRUE,TRUE,NULL,@now,@now),
 ('62000000-0000-0000-0000-000000000005','INSTAGRAM','Instagram','INSTAGRAM',5,TRUE,TRUE,NULL,@now,@now),
 ('62000000-0000-0000-0000-000000000006','OTHER','Otro','OTHER',6,TRUE,TRUE,NULL,@now,@now);
INSERT INTO day_of_week (Id, Code, Name, Value, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
 ('64000000-0000-0000-0000-000000000001','MON','Lunes','1',1,TRUE,TRUE,NULL,@now,@now),
 ('64000000-0000-0000-0000-000000000002','TUE','Martes','2',2,TRUE,TRUE,NULL,@now,@now),
 ('64000000-0000-0000-0000-000000000003','WED','Miercoles','3',3,TRUE,TRUE,NULL,@now,@now),
 ('64000000-0000-0000-0000-000000000004','THU','Jueves','4',4,TRUE,TRUE,NULL,@now,@now),
 ('64000000-0000-0000-0000-000000000005','FRI','Viernes','5',5,TRUE,TRUE,NULL,@now,@now),
 ('64000000-0000-0000-0000-000000000006','SAT','Sabado','6',6,TRUE,TRUE,NULL,@now,@now),
 ('64000000-0000-0000-0000-000000000007','SUN','Domingo','7',7,TRUE,TRUE,NULL,@now,@now);

-- ---------------------------------------------------------------------
-- Distribucion del APK: historico de versiones subidas por el admin.
-- El binario vive en disco (system-service, app.apk.storage-dir); aqui la
-- metadata + checksum. Solo UNA version IsCurrent (la publicada/vigente);
-- publicar sincroniza la constante VERAPP. VersionCode es el androide
-- (creciente obligatorio para que el telefono actualice en sitio sin
-- perder la data local).
-- ---------------------------------------------------------------------
CREATE TABLE app_version (
    Id          CHAR(36)     NOT NULL,
    Version     VARCHAR(20)  NOT NULL,
    VersionCode INT          NOT NULL,
    FileName    VARCHAR(160) NOT NULL,
    Checksum    VARCHAR(64)  NOT NULL,
    SizeBytes   BIGINT       NOT NULL,
    Notes       VARCHAR(500) NULL,
    IsCurrent   BOOLEAN      NOT NULL DEFAULT FALSE,
    Enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy   CHAR(36)     NULL,
    AuditUser   CHAR(36)     NULL,
    AuditDate   DATETIME(6)  NOT NULL,
    CreatedDate DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_app_version_version (Version),
    UNIQUE KEY uq_app_version_code (VersionCode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Permisos de Elasticsearch (ambos asignados a ADMIN).
-- THIRDPARTY_REINDEX es el viejo, de cuando reindexar era una accion escondida
-- en la pantalla de terceros; se queda por compatibilidad. La pantalla nueva
-- cubre TODOS los indices con tres granularidades y pide ELASTIC_MANAGE.
-- ---------------------------------------------------------------------
INSERT INTO permission (Id, Code, Name, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('22222222-0000-0000-0000-000000000007', 'THIRDPARTY_REINDEX', 'Reindexar tercero', 'Permite reindexar un tercero en Elasticsearch', TRUE, TRUE, NULL, @now, @now),
    ('22222222-0000-0000-0000-000000000008', 'ELASTIC_MANAGE', 'Gestionar Elasticsearch', 'Permite consultar el estado de los indices y reindexar registros, entidades o todo', TRUE, TRUE, NULL, @now, @now);
INSERT INTO role_permission (Id, RoleId, PermissionId, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('33333333-0000-0000-0000-0000000000a7', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000007', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-0000000000a8', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000008', TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- Permisos de notificaciones (Fase B, ambos asignados a ADMIN). events-service
-- hoy filtra sus controladores por hasRole('ADMIN'), no por estos codigos:
-- quedan declarados para el panel y para un ajuste fino futuro.
-- ---------------------------------------------------------------------
INSERT INTO permission (Id, Code, Name, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('99992000-0000-0000-0000-000000000001', 'NOTIFICATION_VIEW',   'Ver notificaciones',        'Consultar notificaciones, plantillas, parámetros y bitácora.', TRUE, TRUE, NULL, @now, @now),
    ('99992000-0000-0000-0000-000000000002', 'NOTIFICATION_MANAGE', 'Gestionar notificaciones',  'Crear y editar notificaciones, plantillas y parámetros, y cambiar la configuración de envío.', TRUE, TRUE, NULL, @now, @now);
INSERT INTO role_permission (Id, RoleId, PermissionId, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('33333333-0000-0000-0000-0000000000b1', '11111111-0000-0000-0000-000000000001', '99992000-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now),
    ('33333333-0000-0000-0000-0000000000b2', '11111111-0000-0000-0000-000000000001', '99992000-0000-0000-0000-000000000002', TRUE, TRUE, NULL, @now, @now);

-- ---------------------------------------------------------------------
-- Tour guiado configurable.
--
-- Cada fila es un paso del recorrido de bienvenida. El paso resuelve a QUE
-- apunta en cascada:
--   1) MenuId no nulo  -> foco sobre el elemento del menu, ancla "nav-{Code}".
--   2) Anchor no nulo  -> foco sobre un ancla literal del DOM (los KPIs del
--                         panel, que no son un menu y nunca lo seran).
--   3) ninguno         -> diapositiva a pantalla completa (saludo, cierre).
--
-- El filtrado por rol NO se guarda aqui: se hereda del menu asociado, que ya
-- pasa por menu_role. Un paso cuyo menu no ve el usuario, desaparece.
-- ---------------------------------------------------------------------
CREATE TABLE tour_step (
    Id CHAR(36) NOT NULL,
    -- ON DELETE SET NULL: si el admin borra el menu, el paso degrada a
    -- diapositiva en vez de quedar apuntando a una referencia rota.
    MenuId CHAR(36) NULL,
    Anchor VARCHAR(60) NULL,
    Title VARCHAR(160) NOT NULL,
    Body VARCHAR(500) NOT NULL,
    Icon VARCHAR(60) NULL,
    DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    KEY idx_tour_step_order (DisplayOrder),
    CONSTRAINT fk_tour_step_menu FOREIGN KEY (MenuId) REFERENCES menu (Id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Recorrido inicial. Sin esto el tour arranca vacio en una instalacion nueva.
-- Los pasos 4 y 5 apuntan a menus sembrados mas arriba en este mismo script,
-- asi que van con su Id directo.
INSERT INTO tour_step (Id, MenuId, Anchor, Title, Body, Icon, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
('77772001-0000-0000-0000-000000000001', NULL, NULL,
 '¡Te damos la bienvenida!',
 'Tu negocio, tu equipo y tus cuentas en un solo lugar. Deja que te muestre lo esencial.',
 'sparkles', 1, TRUE, TRUE, NULL, @now, @now),
('77772001-0000-0000-0000-000000000002', NULL, NULL,
 'Todo tu negocio, ordenado',
 'Sedes, servicios, equipo y pagos viven aquí. Nada de hojas de cálculo sueltas.',
 'building-2', 2, TRUE, TRUE, NULL, @now, @now),
('77772001-0000-0000-0000-000000000003', NULL, 'kpis',
 'Tu resumen de un vistazo',
 'Cuántas sedes, cuánta gente y cuántos servicios tienes. Toca cualquiera para ir al detalle.',
 'activity', 3, TRUE, TRUE, NULL, @now, @now),
('77772001-0000-0000-0000-000000000004', '77771001-0000-0000-0000-000000000007', NULL,
 'Tu equipo',
 'Das de alta a cada persona y ellos entran por la app móvil con su correo y contraseña.',
 'users', 4, TRUE, TRUE, NULL, @now, @now),
('77772001-0000-0000-0000-000000000005', '77771001-0000-0000-0000-000000000012', NULL,
 'Pagarle a tu equipo',
 'Aquí ves lo que le debes a cada empleado y confirmas su liquidación.',
 'banknote', 5, TRUE, TRUE, NULL, @now, @now),
('77772001-0000-0000-0000-000000000006', NULL, NULL,
 '¿Empezamos?',
 'Estos son los pasos mínimos para operar. Hazlos ahora o más tarde, a tu ritmo.',
 'circle-check', 6, TRUE, TRUE, NULL, @now, @now);

-- =====================================================================
-- CARGO DE SERVICIO: la unidad que se aprueba o se descarta al liquidar.
-- ---------------------------------------------------------------------
-- Antes se liquidaba un MONTO TOTAL y el dueno no podia saber que estaba
-- pagando, ni rechazar un servicio suelto, ni comprobar que la transferencia
-- de un cliente llego. Ahora la liquidacion es la SUMA DE LO APROBADO.
--
-- AppointmentId va NULO Y SIN CLAVE FORANEA a proposito: el modulo de citas
-- todavia no existe. La columna esta desde hoy para que el dia que llegue se
-- rellene sin migrar nada.
--
-- Los tres importes se guardan CONGELADOS en vez de calcular el neto al vuelo:
-- employee_compensation tiene ValidFrom/ValidTo, asi que recalcular un servicio
-- de marzo con la tarifa de julio daria un numero distinto al que se acordo.
-- Es el mismo motivo por el que employee_settlement guarda BalanceBefore.
-- =====================================================================

CREATE TABLE service_charge (
    Id             CHAR(36)      NOT NULL,
    BusinessId     CHAR(36)      NOT NULL,
    BranchId       CHAR(36)      NULL,
    EmployeeId     CHAR(36)      NOT NULL,
    AppointmentId  CHAR(36)      NULL,

    ServiceName    VARCHAR(160)  NOT NULL,
    ServiceDate    DATE          NOT NULL,
    StartTime      TIME          NULL,
    EndTime        TIME          NULL,

    ClientName     VARCHAR(160)  NULL,
    ClientThirdPartyId CHAR(36)  NULL,
    -- A donde se le manda SU factura. Van sueltos y no via third_party porque
    -- el cliente de un salon suele entrar sin cuenta: exigirle registro para
    -- poder mandarle su factura seria cambiar el negocio por el modelo de datos.
    ClientEmail    VARCHAR(150)  NULL,
    ClientPhone    VARCHAR(30)   NULL,

    GrossAmount    DECIMAL(14,2) NOT NULL,
    DeductionRate  DECIMAL(5,2)  NOT NULL,
    DeductionAmount DECIMAL(14,2) NOT NULL,
    NetAmount      DECIMAL(14,2) NOT NULL,
    Currency       VARCHAR(3)    NOT NULL DEFAULT 'COP',

    -- CASH | TRANSFER | CARD. Los dos electronicos exigen comprobante.
    PaymentMethod  VARCHAR(16)   NOT NULL,
    ReceiptUrl     VARCHAR(500)  NULL,
    ResultPhotoUrl VARCHAR(500)  NULL,

    -- SCHEDULED | PENDING | CONFIRMED | DISCARDED.
    --   SCHEDULED  la cita existe, el servicio todavia no se presto. No entra en
    --              la lista de aprobacion: no hay nada que aprobar hasta que pase.
    --   PENDING    el empleado lo marco terminado; espera decision del dueño.
    --   CONFIRMED  aprobado, entra en la proxima liquidacion.
    --   DISCARDED  rechazado. NO se borra: vive en el historial con su motivo,
    --              porque un servicio que se esfuma sin rastro es una discusion
    --              asegurada con el empleado.
    Status         VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    ConfirmedAt    DATETIME(6)   NULL,
    DiscardedAt    DATETIME(6)   NULL,
    DiscardReason  VARCHAR(300)  NULL,

    -- Liquidacion que pago este cargo. Es lo que hace auditable el pago:
    -- sin esto, "por que le pague esto" no tiene respuesta en tres meses.
    SettlementId   CHAR(36)      NULL,

    Enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN     NOT NULL DEFAULT TRUE,
    CreatedBy   CHAR(36)    NULL,
    AuditUser   CHAR(36)    NULL,
    AuditDate   DATETIME(6) NOT NULL,
    CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    -- La consulta que manda: los cargos de un empleado en un rango de fechas.
    KEY idx_sc_employee_date (EmployeeId, ServiceDate),
    KEY idx_sc_business_status (BusinessId, Status),
    KEY idx_sc_settlement (SettlementId),
    -- Un cargo por cita, y no mas. Es lo que hace idempotente al sincronizador
    -- que crea los cargos desde la agenda: si corre dos veces, el segundo
    -- intento choca contra este indice en vez de pagarle dos veces al empleado.
    -- Nulable a proposito: un cargo suelto (sin cita) no tiene con quien
    -- chocar, y en MySQL varios NULL conviven en un indice unico.
    UNIQUE KEY uq_sc_appointment (AppointmentId),
    CONSTRAINT fk_sc_settlement FOREIGN KEY (SettlementId)
        REFERENCES employee_settlement (Id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- #####################################################################
-- #  FASE C — FLUJOS CONFIGURABLES, AGENDA, MARKETPLACE Y RESENAS
-- #####################################################################

SET @now = NOW(6);

-- =====================================================================
-- FLUJOS CONFIGURABLES
-- ---------------------------------------------------------------------
-- Un FLUJO (AGEND) es un proceso del sistema. Dentro tiene SECCIONES en
-- orden (DATBAS, SERVIC, ...), y cada seccion tiene CAMPOS (que datos se
-- piden) y CONTROLES (que botones hay y que hacen).
--
-- Todas las pantallas que ejecutan el mismo proceso leen la MISMA
-- configuracion: la web publica, el panel, el APK y el formulario
-- conversado de WhatsApp. Por eso el agendamiento pide lo mismo por los
-- cuatro lados sin que nadie tenga que acordarse de replicar un cambio.
--
-- DOS DECISIONES QUE SOSTIENEN ESTO
--
-- 1. IsSystem. Si todo fuera editable, borrar la seccion del calendario
--    dejaria el agendamiento sin fecha y la aplicacion no daria ningun
--    error: simplemente dejaria de funcionar. Lo marcado como de sistema
--    se puede REORDENAR y REETIQUETAR, nunca borrar ni desactivar. Es la
--    diferencia entre configurable y rompible.
--
-- 2. Los permisos NO son una tabla nueva. Un control declara el codigo
--    del permiso que exige (`PermissionCode`) y quien lo tiene se decide
--    con `role_permission`, que ya existe, ya viaja en el token y ya
--    tiene pantalla. Un segundo mecanismo de permisos conviviendo con el
--    actual es la forma mas rapida de que un dia no coincidan.
-- =====================================================================

CREATE TABLE flow (
    Id          CHAR(36)     NOT NULL,
    Code        VARCHAR(40)  NOT NULL,
    Name        VARCHAR(120) NOT NULL,
    Description VARCHAR(500) NULL,
    -- Los flujos del sistema no se borran: hay codigo que los invoca por
    -- su codigo y sin ellos ese codigo no tiene que ejecutar.
    IsSystem    BOOLEAN      NOT NULL DEFAULT FALSE,
    Enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy   CHAR(36)     NULL,
    AuditUser   CHAR(36)     NULL,
    AuditDate   DATETIME(6)  NOT NULL,
    CreatedDate DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_flow_code (Code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE flow_section (
    Id           CHAR(36)     NOT NULL,
    FlowId       CHAR(36)     NOT NULL,
    Code         VARCHAR(40)  NOT NULL,
    Name         VARCHAR(120) NOT NULL,
    Description  VARCHAR(500) NULL,
    -- El ORDEN de las secciones es el orden de los pasos. No hay otra
    -- tabla de "pasos": un paso es una seccion, y su numero es este.
    DisplayOrder INT          NOT NULL DEFAULT 0,

    -- Que clase de seccion es. Solo FORM lee campos; el resto los ignora.
    --   FORM       pide datos (los define flow_field)
    --   SELECTION  se elige de una lista que calcula el servidor
    --              (servicios, empleados, horas libres)
    --   REVIEW     resumen antes de confirmar
    --   INFO       solo informa (el codigo publico al final)
    Kind         VARCHAR(16)  NOT NULL DEFAULT 'FORM',

    -- Donde aplica esta seccion, en CSV: WEB,PANEL,APK,WHATSAPP.
    -- No es adorno: WhatsApp no puede pintar un calendario, asi que la
    -- seccion de fecha se declara para los otros tres y el bot la resuelve
    -- con una lista de opciones. Sin esto, el bot intentaria renderizar un
    -- selector de fecha en un mensaje de texto.
    Channels     VARCHAR(120) NOT NULL DEFAULT 'WEB,PANEL,APK,WHATSAPP',

    IsSystem     BOOLEAN      NOT NULL DEFAULT FALSE,
    Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy    CHAR(36)     NULL,
    AuditUser    CHAR(36)     NULL,
    AuditDate    DATETIME(6)  NOT NULL,
    CreatedDate  DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_flow_section (FlowId, Code),
    KEY idx_fs_flow_order (FlowId, DisplayOrder),
    CONSTRAINT fk_fs_flow FOREIGN KEY (FlowId) REFERENCES flow (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE flow_field (
    Id           CHAR(36)     NOT NULL,
    SectionId    CHAR(36)     NOT NULL,
    -- La clave con la que el dato viaja al servidor ("phone", "notes").
    Code         VARCHAR(60)  NOT NULL,
    Label        VARCHAR(120) NOT NULL,
    Placeholder  VARCHAR(120) NULL,
    HelpText     VARCHAR(300) NULL,

    -- Tipo del dato. Coincide con los `FieldType` del formulario dinamico
    -- del front, que ya sabe pintar todos: text, email, number, money,
    -- textarea, select, multiselect, date, time, checkbox, switch.
    DataType     VARCHAR(20)  NOT NULL DEFAULT 'text',

    -- Mascara del ESTANDAR (shared/forms/core/masks.ts y su gemelo en
    -- Flutter): phone, docId, money, date, email... NULL = texto libre.
    -- Configurar el tipo de dato basta: de ahi salen tambien el teclado
    -- del movil, el tope de caracteres y el ejemplo del placeholder.
    MaskCode     VARCHAR(20)  NULL,

    -- De donde salen las opciones de un select. Un codigo de catalogo
    -- ("document_type") o una clave que el servidor sabe resolver
    -- ("offerings", "employees"). NULL cuando no aplica.
    SourceKey    VARCHAR(60)  NULL,

    IsRequired   BOOLEAN      NOT NULL DEFAULT FALSE,
    -- full | half | third | quarter. Solo lo usan las pantallas anchas.
    Width        VARCHAR(10)  NOT NULL DEFAULT 'full',
    DisplayOrder INT          NOT NULL DEFAULT 0,

    IsSystem     BOOLEAN      NOT NULL DEFAULT FALSE,
    Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy    CHAR(36)     NULL,
    AuditUser    CHAR(36)     NULL,
    AuditDate    DATETIME(6)  NOT NULL,
    CreatedDate  DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_flow_field (SectionId, Code),
    KEY idx_ff_section_order (SectionId, DisplayOrder),
    CONSTRAINT fk_ff_section FOREIGN KEY (SectionId) REFERENCES flow_section (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE flow_control (
    Id           CHAR(36)     NOT NULL,
    SectionId    CHAR(36)     NOT NULL,
    Code         VARCHAR(40)  NOT NULL,
    Label        VARCHAR(120) NOT NULL,

    -- Que hace el boton. El front mapea la accion a su manejador; el
    -- texto y el color son configuracion, el comportamiento no.
    --   NEXT BACK SAVE CONFIRM CANCEL EDIT DELETE CUSTOM
    Action       VARCHAR(20)  NOT NULL,
    -- primary | secondary | ghost | ink | danger (variantes del sistema).
    Variant      VARCHAR(16)  NOT NULL DEFAULT 'primary',

    -- Permiso que exige. NULL = cualquiera que llegue a la pantalla.
    -- Se resuelve contra `permission` + `role_permission`, que ya existen:
    -- este modulo NO trae su propio mecanismo de permisos.
    PermissionCode VARCHAR(50) NULL,

    -- Donde aparece, en CSV. El boton de "registrar cita pasada" tiene
    -- sentido en el panel y en el APK, no en la web publica.
    Channels     VARCHAR(120) NOT NULL DEFAULT 'WEB,PANEL,APK,WHATSAPP',

    DisplayOrder INT          NOT NULL DEFAULT 0,
    IsSystem     BOOLEAN      NOT NULL DEFAULT FALSE,
    Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    CreatedBy    CHAR(36)     NULL,
    AuditUser    CHAR(36)     NULL,
    AuditDate    DATETIME(6)  NOT NULL,
    CreatedDate  DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_flow_control (SectionId, Code),
    KEY idx_fc_section_order (SectionId, DisplayOrder),
    CONSTRAINT fk_fc_section FOREIGN KEY (SectionId) REFERENCES flow_section (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- TEXTOS DE FLUJO
-- ---------------------------------------------------------------------
-- Sueltos a proposito: NO se relacionan con una seccion ni con un
-- control. Se configuran aqui y quien los necesite los pide POR CODIGO
-- —el front, el APK y el bot de WhatsApp—, igual que se pide una
-- traduccion.
--
-- Y estan separados de `notification_template` (que vive en saas_events)
-- porque son otra cosa: una plantilla de notificacion se ASOCIA a una
-- notificacion y sale por un canal; esto es el texto que se pinta o se
-- dice dentro de un proceso.
-- ---------------------------------------------------------------------
CREATE TABLE flow_message (
    Id          CHAR(36)      NOT NULL,
    Code        VARCHAR(60)   NOT NULL,
    Name        VARCHAR(120)  NOT NULL,
    Body        VARCHAR(1000) NOT NULL,
    Description VARCHAR(300)  NULL,
    Enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN       NOT NULL DEFAULT TRUE,
    CreatedBy   CHAR(36)      NULL,
    AuditUser   CHAR(36)      NULL,
    AuditDate   DATETIME(6)   NOT NULL,
    CreatedDate DATETIME(6)   NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_flow_message_code (Code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- POLITICAS DE RESERVA POR NEGOCIO
-- ---------------------------------------------------------------------
-- Una fila por negocio. Van aqui y no como constantes en codigo porque
-- una barberia con dos sillas y un centro de estetica con cabinas no
-- reservan igual, y quien lo sabe es el dueno, no el programador.
-- =====================================================================
CREATE TABLE business_booking_policy (
    Id         CHAR(36) NOT NULL,
    BusinessId CHAR(36) NOT NULL,

    -- FALSE = la cita nace CONFIRMADA. TRUE = nace PENDIENTE_CONFIRMACION
    -- y alguien del negocio tiene que aceptarla.
    RequiresManualConfirmation BOOLEAN NOT NULL DEFAULT FALSE,
    -- Cuanto espera una cita pendiente antes de EXPIRAR.
    ConfirmationTimeoutMinutes INT     NOT NULL DEFAULT 120,

    -- No se reserva con menos de esta antelacion. Evita la cita para
    -- "dentro de cinco minutos" cuando el local esta a media hora.
    MinLeadTimeMinutes         INT     NOT NULL DEFAULT 30,
    -- Ni mas alla de este horizonte. Sin tope, alguien reserva para
    -- dentro de dos anos y esa franja queda muerta.
    MaxHorizonDays             INT     NOT NULL DEFAULT 60,

    -- Cada cuanto empieza un hueco: 15 min significa :00 :15 :30 :45.
    SlotGranularityMinutes     INT     NOT NULL DEFAULT 15,
    -- Preparacion y limpieza. Se restan del hueco, no del servicio.
    BufferBeforeMinutes        INT     NOT NULL DEFAULT 0,
    BufferAfterMinutes         INT     NOT NULL DEFAULT 0,

    -- Hasta cuantas horas antes puede cancelar el CLIENTE por su cuenta.
    ClientCancelWindowHours    INT     NOT NULL DEFAULT 4,

    -- Exigir telefono verificado para reservar desde la web publica. Por
    -- defecto SI: sin esto cualquiera llena la agenda con numeros inventados y
    -- el negocio se entera cuando no llega nadie. Se puede apagar, pero es una
    -- decision del dueno y queda escrita.
    RequirePhoneVerification   BOOLEAN NOT NULL DEFAULT TRUE,

    -- Interruptor general del canal de WhatsApp para este negocio.
    WhatsappEnabled            BOOLEAN NOT NULL DEFAULT FALSE,
    -- Horas antes a las que se recuerda, separadas por coma ("24,2").
    -- Vacio = ese negocio no manda recordatorios.
    ReminderHoursBefore        VARCHAR(40) NOT NULL DEFAULT '24',
    -- Cuantos mensajes lleva enviados este mes. Meta cobra por mensaje:
    -- sin medirlo, la factura llega antes que la conversacion sobre si
    -- compensa.
    WhatsappMonthlyCount       INT     NOT NULL DEFAULT 0,
    WhatsappCountResetAt       DATETIME(6) NULL,

    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_bbp_business (BusinessId),
    CONSTRAINT fk_bbp_business FOREIGN KEY (BusinessId) REFERENCES business (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- QUE SERVICIO PRESTA QUE EMPLEADO
-- ---------------------------------------------------------------------
-- Sin esta tabla no se puede calcular disponibilidad: hasta ahora el
-- vinculo era indirecto por ESPECIALIDAD (employee.SpecialtyId contra
-- business_offering.SpecialtyId), que sirve para agrupar pero no para
-- decir "este barbero hace barba pero no color". Y era una sola
-- especialidad por persona.
-- =====================================================================
CREATE TABLE employee_offering (
    Id         CHAR(36) NOT NULL,
    EmployeeId CHAR(36) NOT NULL,
    OfferingId CHAR(36) NOT NULL,
    -- Sobreescrituras del empleado. NULL = hereda del servicio. El
    -- aprendiz tarda mas en el mismo corte; el maestro cobra otra
    -- comision. Nulables para no repetir el valor heredado en cada fila.
    DurationMinutes INT           NULL,
    CommissionRate  DECIMAL(5,2)  NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_eo_employee_offering (EmployeeId, OfferingId),
    KEY idx_eo_offering (OfferingId),
    CONSTRAINT fk_eo_employee FOREIGN KEY (EmployeeId) REFERENCES employee (Id),
    CONSTRAINT fk_eo_offering FOREIGN KEY (OfferingId) REFERENCES business_offering (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- EXCEPCIONES DE AGENDA
-- ---------------------------------------------------------------------
-- Todo lo que RESTA tiempo del horario normal: un festivo, unas
-- vacaciones, una incapacidad, o bloquear la hora del almuerzo de un
-- martes concreto.
--
-- Con rango de fecha-HORA, no por dia completo: bloquear "de 2 a 4" es
-- el caso mas frecuente y el que un modelo por dias no sabe expresar.
--
-- BranchId y EmployeeId nulables y excluyentes en la practica:
--   ambos NULL   -> afecta a todo el negocio (un festivo)
--   BranchId     -> cierra esa sede
--   EmployeeId   -> ausencia de esa persona
-- =====================================================================
CREATE TABLE agenda_exception (
    Id         CHAR(36) NOT NULL,
    BusinessId CHAR(36) NOT NULL,
    BranchId   CHAR(36) NULL,
    EmployeeId CHAR(36) NULL,
    -- En UTC, como la agenda entera. La vista local se reconstruye con la
    -- zona horaria del negocio.
    StartUtc   DATETIME(6) NOT NULL,
    EndUtc     DATETIME(6) NOT NULL,
    -- HOLIDAY | VACATION | SICK | BLOCK | OTHER
    Kind       VARCHAR(16)  NOT NULL DEFAULT 'BLOCK',
    Reason     VARCHAR(200) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    -- La consulta del motor: "que resta en este negocio en este rango".
    KEY idx_ax_business_range (BusinessId, StartUtc, EndUtc),
    KEY idx_ax_employee_range (EmployeeId, StartUtc, EndUtc),
    CONSTRAINT fk_ax_business FOREIGN KEY (BusinessId) REFERENCES business (Id),
    CONSTRAINT fk_ax_branch FOREIGN KEY (BranchId) REFERENCES branch (Id),
    CONSTRAINT fk_ax_employee FOREIGN KEY (EmployeeId) REFERENCES employee (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- FESTIVOS
-- ---------------------------------------------------------------------
-- Tabla cargable, NO fechas escritas en codigo. Los festivos
-- colombianos son moviles: la Ley Emiliani traslada casi todos al lunes
-- siguiente, y los de Semana Santa dependen de la Pascua. Una constante
-- en el codigo caduca cada diciembre sin avisar.
--
-- Un festivo NO bloquea la agenda por si solo: hay barberias que abren
-- el 20 de julio. El negocio decide, y cuando decide cerrar se genera
-- una `agenda_exception` de tipo HOLIDAY a partir de esta tabla.
-- =====================================================================
CREATE TABLE public_holiday (
    Id          CHAR(36)     NOT NULL,
    CountryCode CHAR(2)      NOT NULL DEFAULT 'CO',
    HolidayDate DATE         NOT NULL,
    Name        VARCHAR(120) NOT NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_holiday_country_date (CountryCode, HolidayDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- LA CITA
-- =====================================================================
CREATE TABLE appointment (
    Id               CHAR(36) NOT NULL,
    BusinessId       CHAR(36) NOT NULL,
    BranchId         CHAR(36) NOT NULL,
    EmployeeId       CHAR(36) NOT NULL,
    BusinessClientId CHAR(36) NOT NULL,

    -- Desde donde se creo: WEB_PUBLICA | PANEL_DUENO | APK_EMPLEADO |
    -- WHATSAPP | MARKETPLACE. No es estadistica: la politica de quien
    -- puede saltarse que regla depende del origen.
    Channel          VARCHAR(16) NOT NULL,
    -- Nulo cuando la creo el cliente sin cuenta desde la web publica.
    CreatedByUserId  CHAR(36) NULL,

    -- SIEMPRE en UTC. Toda comparacion del motor ocurre aqui.
    StartUtc         DATETIME(6) NOT NULL,
    EndUtc           DATETIME(6) NOT NULL,
    -- La zona del negocio EN EL MOMENTO de agendar. Se congela para poder
    -- reconstruir la vista local historica: si el negocio se muda de
    -- huso, las citas viejas siguen contando la hora a la que ocurrieron.
    BusinessTimeZone VARCHAR(64) NOT NULL DEFAULT 'America/Bogota',
    -- La fecha LOCAL del negocio, derivada de StartUtc. Existe por dos
    -- razones: es la clave del bloqueo anti doble reserva, y es como se
    -- consulta la agenda ("el dia 5"), que en UTC seria un rango raro.
    LocalDate        DATE        NOT NULL,

    Status           VARCHAR(28) NOT NULL,
    -- Bloqueo optimista: dos pantallas editando la misma cita no se pisan.
    Version          INT         NOT NULL DEFAULT 0,

    -- Codigo corto y NO secuencial para que el cliente sin cuenta consulte
    -- o cancele. Nunca se expone el id interno.
    PublicCode       VARCHAR(12) NOT NULL,

    -- LA DISTINCION QUE GOBIERNA EL ANTI DOBLE RESERVA
    -- ----------------------------------------------------------------
    -- FALSE = se esta AGENDANDO algo futuro. Exclusivo: una franja, una
    --         persona. Si hay solape, se rechaza.
    -- TRUE  = se esta REGISTRANDO algo que YA ocurrio. Puede solaparse
    --         con lo que hubiera: el pasado no se negocia, se anota.
    --
    -- Son dos operaciones distintas con validaciones distintas, no una
    -- bandera de "saltarse la comprobacion". El panel puede registrar
    -- retroactivo; agendar a futuro sigue siendo exclusivo para todos,
    -- dueno incluido.
    IsBackdated      BOOLEAN     NOT NULL DEFAULT FALSE,

    -- Reprogramar NO mueve fechas: cancela con motivo REPROGRAMADA y crea
    -- una cita nueva que apunta aqui. Asi la historia queda entera.
    RescheduledFromAppointmentId CHAR(36) NULL,

    CancelReason     VARCHAR(300) NULL,
    -- CLIENT | BUSINESS | SYSTEM
    CancelledBy      VARCHAR(16)  NULL,
    CancelledAt      DATETIME(6)  NULL,
    -- Momento real en que se marco en curso y terminada.
    StartedAt        DATETIME(6)  NULL,
    CompletedAt      DATETIME(6)  NULL,

    -- Totales calculados de las lineas. El detalle y su snapshot viven en
    -- appointment_service.
    TotalPrice       DECIMAL(14,2) NOT NULL DEFAULT 0,
    TotalDurationMinutes INT       NOT NULL DEFAULT 0,
    Currency         VARCHAR(3)    NOT NULL DEFAULT 'COP',
    Notes            VARCHAR(500)  NULL,

    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_appt_public_code (PublicCode),
    -- EL indice del solapamiento. El orden importa: se filtra por empleado
    -- y por estado, y se recorre por rango.
    KEY idx_appt_overlap (EmployeeId, StartUtc, EndUtc, Status),
    KEY idx_appt_business_day (BusinessId, LocalDate),
    KEY idx_appt_branch_day (BranchId, LocalDate),
    KEY idx_appt_client (BusinessClientId),
    CONSTRAINT fk_appt_business FOREIGN KEY (BusinessId) REFERENCES business (Id),
    CONSTRAINT fk_appt_branch FOREIGN KEY (BranchId) REFERENCES branch (Id),
    CONSTRAINT fk_appt_employee FOREIGN KEY (EmployeeId) REFERENCES employee (Id),
    CONSTRAINT fk_appt_client FOREIGN KEY (BusinessClientId) REFERENCES business_client (Id),
    CONSTRAINT fk_appt_rescheduled FOREIGN KEY (RescheduledFromAppointmentId)
        REFERENCES appointment (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- LOS SERVICIOS DE UNA CITA, CON SU FOTO DEL MOMENTO
-- ---------------------------------------------------------------------
-- Una cita puede llevar varios (corte + barba), asi que el snapshot va
-- AQUI y `appointment` guarda solo los totales.
--
-- Los importes se congelan y esto NO es duplicar datos: si el dueno sube
-- el precio del corte en julio, las citas de marzo no pueden cambiar de
-- valor, porque de ahi salieron comisiones que ya se pagaron. La FK al
-- servicio se mantiene para trazabilidad; el numero que manda es este.
-- ---------------------------------------------------------------------
CREATE TABLE appointment_service (
    Id            CHAR(36) NOT NULL,
    AppointmentId CHAR(36) NOT NULL,
    -- Referencia para trazar. Puede quedar apuntando a un servicio que
    -- despues se deshabilito: por eso el nombre tambien se congela.
    OfferingId    CHAR(36) NULL,
    ServiceName   VARCHAR(160)  NOT NULL,
    Price         DECIMAL(14,2) NOT NULL,
    DurationMinutes INT         NOT NULL,
    -- % de comision del empleado, resuelto de la compensacion vigente al
    -- agendar, y su importe ya calculado.
    CommissionRate   DECIMAL(5,2)  NOT NULL DEFAULT 0,
    CommissionAmount DECIMAL(14,2) NOT NULL DEFAULT 0,
    DisplayOrder  INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    KEY idx_as_appointment (AppointmentId),
    KEY idx_as_offering (OfferingId),
    CONSTRAINT fk_as_appointment FOREIGN KEY (AppointmentId)
        REFERENCES appointment (Id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- HISTORIAL DE LA CITA
-- ---------------------------------------------------------------------
-- Solo se anade, nunca se modifica ni se borra. Sin esto no hay forma de
-- responder "quien cancelo esto y cuando", que es exactamente la
-- pregunta que aparece cuando un cliente reclama.
-- ---------------------------------------------------------------------
CREATE TABLE appointment_history (
    Id            CHAR(36) NOT NULL,
    AppointmentId CHAR(36) NOT NULL,
    -- NULL en la primera fila: la cita no venia de ningun estado.
    FromStatus    VARCHAR(28)  NULL,
    ToStatus      VARCHAR(28)  NOT NULL,
    Channel       VARCHAR(16)  NOT NULL,
    -- Nulo si lo hizo el cliente sin cuenta o un proceso automatico.
    ActorUserId   CHAR(36)     NULL,
    Reason        VARCHAR(300) NULL,
    OccurredAt    DATETIME(6)  NOT NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    KEY idx_ah_appointment (AppointmentId, OccurredAt),
    CONSTRAINT fk_ah_appointment FOREIGN KEY (AppointmentId) REFERENCES appointment (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- DEDUPLICACION DE AVISOS
-- ---------------------------------------------------------------------
-- Una fila por (cita, tipo de aviso), con clave unica. Si el barrido de
-- recordatorios corre dos veces —o dos instancias corren a la vez— el
-- segundo INSERT choca contra el indice y no sale un segundo mensaje.
-- Que la idempotencia la garantice la BASE y no un `if` es lo que la
-- hace cierta tambien cuando hay dos procesos.
--
-- Es una tabla y no una columna por tipo porque cuantos recordatorios
-- manda un negocio es CONFIGURABLE: con columnas, anadir "tambien 2
-- horas antes" seria una migracion.
-- =====================================================================
CREATE TABLE appointment_notification (
    Id               CHAR(36)    NOT NULL,
    AppointmentId    CHAR(36)    NOT NULL,
    -- El codigo de la notificacion en events-service (APPOINTMENT_REMINDER...).
    -- Para los recordatorios lleva ademas las horas ("APPOINTMENT_REMINDER:24"),
    -- porque el de 24 horas y el de 2 son avisos distintos.
    NotificationCode VARCHAR(80) NOT NULL,
    SentAt           DATETIME(6) NOT NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_appt_notif (AppointmentId, NotificationCode),
    CONSTRAINT fk_appt_notif_appointment FOREIGN KEY (AppointmentId) REFERENCES appointment (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- EL CERROJO DE LA AGENDA
-- ---------------------------------------------------------------------
-- Una fila por empleado y dia local. Toda reserva la bloquea PRIMERO con
-- SELECT ... FOR UPDATE, y solo despues comprueba solapamiento e
-- inserta. Dos reservas simultaneas al mismo hueco se serializan aqui:
-- la segunda entra cuando la primera ya escribio, ve el solape y falla.
--
-- POR QUE UNA FILA Y NO UN FOR UPDATE SOBRE EL RANGO DE CITAS
-- MySQL 8 en REPEATABLE READ si toma gap locks sobre un rango vacio, y
-- en teoria bastaria. Pero que los tome depende del plan que elija el
-- optimizador para ese indice y ese rango, y un anti doble reserva que
-- falla una vez de cada mil es peor que uno explicito: la fila que se
-- bloquea siempre existe, asi que no hay hueco que discutir.
--
-- Si algun dia se demuestra con una prueba de concurrencia que el gap
-- lock aguanta, quitar esta tabla es mas facil que anadirla despues.
-- =====================================================================
CREATE TABLE agenda_lock (
    Id         CHAR(36) NOT NULL,
    EmployeeId CHAR(36) NOT NULL,
    LocalDate  DATE     NOT NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_al_employee_date (EmployeeId, LocalDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- FICHA EN EL DIRECTORIO (MARKETPLACE)
-- ---------------------------------------------------------------------
-- Aqui vive SOLO lo que existe por el directorio y por nada mas. Todo lo
-- que describe al negocio se LEE de donde ya estaba:
--
--   nombre y logo -> business            (al crear el negocio)
--   categoria     -> business_type, via business.BusinessTypeId
--   subdominio    -> business_domain     (al crear el negocio)
--   titular       -> business_landing.Tagline       ("Mi pagina")
--   descripcion   -> business_landing.About         ("Mi pagina")
--   portada       -> business_landing.HeroImageUrl  ("Mi pagina")
--   direccion     -> branch.AddressLine             ("Sedes")
--   ciudad y mapa -> branch.MunicipalityId / Latitude / Longitude
--
-- Copiarlo aqui era condenarlo a divergir: se cambia la direccion en
-- Sedes y el directorio seguiria enseñando la vieja, sin que nada falle.
-- =====================================================================
CREATE TABLE business_public_profile (
    Id          CHAR(36) NOT NULL,
    BusinessId  CHAR(36) NOT NULL,

    -- OPT-IN. Arranca en FALSE: no todos los negocios quieren salir en un
    -- directorio, y aparecer sin haberlo pedido no es una funcionalidad.
    IsListed   BOOLEAN NOT NULL DEFAULT FALSE,
    IsVerified BOOLEAN NOT NULL DEFAULT FALSE,

    -- AGREGADOS DE CALIFICACION. Desnormalizacion DELIBERADA: calcular el
    -- promedio sobre todas las resenas en cada carga del directorio no
    -- escala. Se actualizan en la MISMA transaccion que crea o modera una
    -- resena, con incremento atomico, y un job de reconciliacion avisa si
    -- alguna vez discrepan del recuento real.
    ReviewCount INT NOT NULL DEFAULT 0,
    StarSum     INT NOT NULL DEFAULT 0,

    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_bpp_business (BusinessId),
    KEY idx_bpp_listed (IsListed),
    CONSTRAINT fk_bpp_business FOREIGN KEY (BusinessId) REFERENCES business (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- RESENAS
-- ---------------------------------------------------------------------
-- Una por cita, y solo si la cita se COMPLETO. La cita ya sabe de que
-- negocio y de que empleado es; BusinessId y EmployeeId se repiten aqui
-- como DESNORMALIZACION DELIBERADA para poder indexar "las resenas de
-- este negocio" sin unir con la agenda en cada carga del directorio.
--
-- El negocio NO puede borrar una resena. Puede responderla una vez y
-- puede reportarla para moderacion. Es una regla del sistema, no de la
-- interfaz: sin ella, la calificacion no vale nada.
-- =====================================================================
CREATE TABLE appointment_review (
    Id            CHAR(36) NOT NULL,
    AppointmentId CHAR(36) NOT NULL,
    BusinessId    CHAR(36) NOT NULL,
    EmployeeId    CHAR(36) NOT NULL,

    -- Se califican las dos cosas en una sola fila: el sitio y quien
    -- atendio. Separarlas en dos entidades duplicaria la moderacion, la
    -- ventana de 14 dias y el indice unico.
    BusinessStars TINYINT NOT NULL,
    EmployeeStars TINYINT NULL,
    Comment       VARCHAR(1000) NULL,

    -- PUBLISHED | HIDDEN | REPORTED. Nunca se borra fisicamente.
    Status        VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED',
    BusinessReply VARCHAR(1000) NULL,
    BusinessRepliedAt DATETIME(6) NULL,

    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL,
    AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    -- Una calificacion por cita. Es la regla, y la impone el indice.
    UNIQUE KEY uq_ar_appointment (AppointmentId),
    KEY idx_ar_business (BusinessId, Status),
    KEY idx_ar_employee (EmployeeId, Status),
    CONSTRAINT fk_ar_appointment FOREIGN KEY (AppointmentId) REFERENCES appointment (Id),
    CONSTRAINT fk_ar_business FOREIGN KEY (BusinessId) REFERENCES business (Id),
    CONSTRAINT fk_ar_employee FOREIGN KEY (EmployeeId) REFERENCES employee (Id),
    CONSTRAINT ck_ar_business_stars CHECK (BusinessStars BETWEEN 1 AND 5),
    CONSTRAINT ck_ar_employee_stars CHECK (EmployeeStars IS NULL OR EmployeeStars BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- SEED: FESTIVOS DE COLOMBIA 2026
-- ---------------------------------------------------------------------
-- Ya trasladados segun la Ley Emiliani (casi todos al lunes siguiente) y
-- con los de Semana Santa calculados sobre la Pascua del 5 de abril.
--
-- SOLO 2026. Los siguientes se cargan desde la pantalla de
-- administracion o por script antes de que empiece el ano: sembrar de
-- memoria varios anos es como se cuelan las fechas mal.
-- =====================================================================
INSERT INTO public_holiday (Id, CountryCode, HolidayDate, Name, Enabled, Visible, AuditDate, CreatedDate) VALUES
('c0000000-0000-0000-0000-000020260101', 'CO', '2026-01-01', 'Año Nuevo',                       TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020260112', 'CO', '2026-01-12', 'Reyes Magos',                      TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020260323', 'CO', '2026-03-23', 'San José',                         TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020260402', 'CO', '2026-04-02', 'Jueves Santo',                     TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020260403', 'CO', '2026-04-03', 'Viernes Santo',                    TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020260501', 'CO', '2026-05-01', 'Día del Trabajo',                  TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020260518', 'CO', '2026-05-18', 'Ascensión del Señor',              TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020260608', 'CO', '2026-06-08', 'Corpus Christi',                   TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020260615', 'CO', '2026-06-15', 'Sagrado Corazón',                  TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020260629', 'CO', '2026-06-29', 'San Pedro y San Pablo',            TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020260720', 'CO', '2026-07-20', 'Día de la Independencia',          TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020260807', 'CO', '2026-08-07', 'Batalla de Boyacá',                TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020260817', 'CO', '2026-08-17', 'Asunción de la Virgen',            TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020261012', 'CO', '2026-10-12', 'Día de la Raza',                   TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020261102', 'CO', '2026-11-02', 'Todos los Santos',                 TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020261116', 'CO', '2026-11-16', 'Independencia de Cartagena',       TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020261208', 'CO', '2026-12-08', 'Inmaculada Concepción',            TRUE, TRUE, @now, @now),
('c0000000-0000-0000-0000-000020261225', 'CO', '2026-12-25', 'Navidad',                          TRUE, TRUE, @now, @now);

-- =====================================================================
-- SEED: EL FLUJO DE AGENDAMIENTO (AGEND)
-- ---------------------------------------------------------------------
-- Nace COMPLETO y marcado como de sistema. La configuracion sirve para
-- reordenar pasos, cambiar etiquetas, anadir campos propios y decidir
-- que rol ve que boton — no para dejar el agendamiento sin calendario.
--
-- Las cuatro superficies leen esto: la web publica del subdominio, el
-- panel del dueno, el APK del empleado y el formulario conversado de
-- WhatsApp. Cada seccion declara en que canales aplica.
-- =====================================================================
INSERT INTO flow (Id, Code, Name, Description, IsSystem, Enabled, Visible, AuditDate, CreatedDate) VALUES
('f1000000-0000-0000-0000-000000000001', 'AGEND', 'Agendamiento de cita',
 'Reservar una cita. Lo ejecutan la web pública, el panel, el APK y WhatsApp con la misma configuración.',
 TRUE, TRUE, TRUE, @now, @now);

INSERT INTO flow_section
    (Id, FlowId, Code, Name, Description, DisplayOrder, Kind, Channels, IsSystem, Enabled, Visible, AuditDate, CreatedDate)
VALUES
('f2000000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000001',
 'SERVIC', 'Servicio', 'Qué se va a hacer. Admite varios (corte + barba).',
 1, 'SELECTION', 'WEB,PANEL,APK,WHATSAPP', TRUE, TRUE, TRUE, @now, @now),

('f2000000-0000-0000-0000-000000000002', 'f1000000-0000-0000-0000-000000000001',
 'PROFES', 'Profesional', 'Con quién. Se puede dejar en "el primero disponible".',
 2, 'SELECTION', 'WEB,PANEL,APK,WHATSAPP', TRUE, TRUE, TRUE, @now, @now),

('f2000000-0000-0000-0000-000000000003', 'f1000000-0000-0000-0000-000000000001',
 'FECHOR', 'Fecha y hora', 'Los huecos que quedan libres de verdad, calculados al momento.',
 3, 'SELECTION', 'WEB,PANEL,APK,WHATSAPP', TRUE, TRUE, TRUE, @now, @now),

('f2000000-0000-0000-0000-000000000004', 'f1000000-0000-0000-0000-000000000001',
 'DATBAS', 'Datos básicos', 'De quién es la cita.',
 4, 'FORM', 'WEB,PANEL,APK,WHATSAPP', TRUE, TRUE, TRUE, @now, @now),

-- Solo donde el cliente se identifica solo. En el panel y en el APK la
-- cita la crea alguien del negocio, que ya responde por ella.
('f2000000-0000-0000-0000-000000000005', 'f1000000-0000-0000-0000-000000000001',
 'VERIFI', 'Verificación del teléfono', 'Un código de un solo uso. Sin esto cualquiera llena la agenda con datos falsos.',
 5, 'FORM', 'WEB,WHATSAPP', TRUE, TRUE, TRUE, @now, @now),

('f2000000-0000-0000-0000-000000000006', 'f1000000-0000-0000-0000-000000000001',
 'CONFIR', 'Confirmación', 'El resumen antes de reservar. Aquí es donde se ve lo que se acaba de elegir.',
 6, 'REVIEW', 'WEB,PANEL,APK,WHATSAPP', TRUE, TRUE, TRUE, @now, @now),

('f2000000-0000-0000-0000-000000000007', 'f1000000-0000-0000-0000-000000000001',
 'LISTO', 'Listo', 'El código con el que se consulta o se cancela después.',
 7, 'INFO', 'WEB,PANEL,APK,WHATSAPP', TRUE, TRUE, TRUE, @now, @now);

-- ----- Campos de "Datos básicos" -----
INSERT INTO flow_field
    (Id, SectionId, Code, Label, Placeholder, HelpText, DataType, MaskCode, SourceKey,
     IsRequired, Width, DisplayOrder, IsSystem, Enabled, Visible, AuditDate, CreatedDate)
VALUES
('f3000000-0000-0000-0000-000000000001', 'f2000000-0000-0000-0000-000000000004',
 'fullName', 'Nombre', NULL, 'Como aparecerá en la agenda del negocio.',
 'text', 'name', NULL, TRUE, 'full', 1, TRUE, TRUE, TRUE, @now, @now),

('f3000000-0000-0000-0000-000000000002', 'f2000000-0000-0000-0000-000000000004',
 'phone', 'Celular', NULL, 'Ahí se envía la confirmación y el recordatorio.',
 'text', 'phone', NULL, TRUE, 'full', 2, TRUE, TRUE, TRUE, @now, @now),

-- No es de sistema: un negocio puede quitarlo si no le sirve.
('f3000000-0000-0000-0000-000000000003', 'f2000000-0000-0000-0000-000000000004',
 'notes', 'Algo que debamos saber', 'Opcional', NULL,
 'textarea', NULL, NULL, FALSE, 'full', 3, FALSE, TRUE, TRUE, @now, @now);

-- ----- Campo de la verificación -----
INSERT INTO flow_field
    (Id, SectionId, Code, Label, Placeholder, HelpText, DataType, MaskCode, SourceKey,
     IsRequired, Width, DisplayOrder, IsSystem, Enabled, Visible, AuditDate, CreatedDate)
VALUES
('f3000000-0000-0000-0000-000000000004', 'f2000000-0000-0000-0000-000000000005',
 'code', 'Código', NULL, 'Los 6 dígitos que acabamos de enviarte.',
 'text', 'otp', NULL, TRUE, 'full', 1, TRUE, TRUE, TRUE, @now, @now);

-- ----- Controles -----
-- El comportamiento lo decide `Action`; el texto y el color son
-- configuracion. `PermissionCode` se resuelve contra `role_permission`.
INSERT INTO flow_control
    (Id, SectionId, Code, Label, Action, Variant, PermissionCode, Channels,
     DisplayOrder, IsSystem, Enabled, Visible, AuditDate, CreatedDate)
VALUES
('f4000000-0000-0000-0000-000000000001', 'f2000000-0000-0000-0000-000000000001',
 'NEXT', 'Continuar', 'NEXT', 'primary', NULL, 'WEB,PANEL,APK,WHATSAPP', 1, TRUE, TRUE, TRUE, @now, @now),

('f4000000-0000-0000-0000-000000000002', 'f2000000-0000-0000-0000-000000000002',
 'BACK', 'Atrás', 'BACK', 'ghost', NULL, 'WEB,PANEL,APK', 1, TRUE, TRUE, TRUE, @now, @now),
('f4000000-0000-0000-0000-000000000003', 'f2000000-0000-0000-0000-000000000002',
 'NEXT', 'Continuar', 'NEXT', 'primary', NULL, 'WEB,PANEL,APK,WHATSAPP', 2, TRUE, TRUE, TRUE, @now, @now),

('f4000000-0000-0000-0000-000000000004', 'f2000000-0000-0000-0000-000000000003',
 'BACK', 'Atrás', 'BACK', 'ghost', NULL, 'WEB,PANEL,APK', 1, TRUE, TRUE, TRUE, @now, @now),
('f4000000-0000-0000-0000-000000000005', 'f2000000-0000-0000-0000-000000000003',
 'NEXT', 'Continuar', 'NEXT', 'primary', NULL, 'WEB,PANEL,APK,WHATSAPP', 2, TRUE, TRUE, TRUE, @now, @now),

('f4000000-0000-0000-0000-000000000006', 'f2000000-0000-0000-0000-000000000004',
 'BACK', 'Atrás', 'BACK', 'ghost', NULL, 'WEB,PANEL,APK', 1, TRUE, TRUE, TRUE, @now, @now),
('f4000000-0000-0000-0000-000000000007', 'f2000000-0000-0000-0000-000000000004',
 'NEXT', 'Continuar', 'NEXT', 'primary', NULL, 'WEB,PANEL,APK,WHATSAPP', 2, TRUE, TRUE, TRUE, @now, @now),

('f4000000-0000-0000-0000-000000000008', 'f2000000-0000-0000-0000-000000000005',
 'RESEND', 'Enviar otro código', 'CUSTOM', 'ghost', NULL, 'WEB,WHATSAPP', 1, TRUE, TRUE, TRUE, @now, @now),
('f4000000-0000-0000-0000-000000000009', 'f2000000-0000-0000-0000-000000000005',
 'NEXT', 'Verificar', 'NEXT', 'primary', NULL, 'WEB,WHATSAPP', 2, TRUE, TRUE, TRUE, @now, @now),

('f4000000-0000-0000-0000-00000000000a', 'f2000000-0000-0000-0000-000000000006',
 'BACK', 'Atrás', 'BACK', 'ghost', NULL, 'WEB,PANEL,APK', 1, TRUE, TRUE, TRUE, @now, @now),
('f4000000-0000-0000-0000-00000000000b', 'f2000000-0000-0000-0000-000000000006',
 'CONFIRM', 'Reservar', 'CONFIRM', 'primary', NULL, 'WEB,PANEL,APK,WHATSAPP', 2, TRUE, TRUE, TRUE, @now, @now),

-- Registrar algo YA PRESTADO. Solo donde hay alguien del negocio
-- detrás, y exige permiso: es la operación que puede solaparse con lo
-- que hubiera en la agenda.
('f4000000-0000-0000-0000-00000000000c', 'f2000000-0000-0000-0000-000000000006',
 'BACKDATE', 'Registrar como ya realizada', 'CUSTOM', 'secondary', 'APPOINTMENT_BACKDATE',
 'PANEL,APK', 3, TRUE, TRUE, TRUE, @now, @now);

-- ----- Textos del flujo -----
-- Sueltos, sin relacion con seccion ni control: se piden POR CODIGO.
INSERT INTO flow_message (Id, Code, Name, Body, Description, Enabled, Visible, AuditDate, CreatedDate) VALUES
('f5000000-0000-0000-0000-000000000001', 'AGEND_SALUDO', 'Saludo del agendamiento',
 'Hola. Te ayudo a reservar tu cita en {{NEGOCIO}}. ¿Qué servicio quieres?',
 'Primer mensaje del formulario conversado por WhatsApp.', TRUE, TRUE, @now, @now),

('f5000000-0000-0000-0000-000000000002', 'AGEND_PIDE_PROFESIONAL', 'Pregunta por el profesional',
 '¿Con quién prefieres? Si te da igual, responde "cualquiera" y te asigno al primero libre.',
 NULL, TRUE, TRUE, @now, @now),

('f5000000-0000-0000-0000-000000000003', 'AGEND_PIDE_HORA', 'Pregunta por la hora',
 'Estas son las horas libres. Responde con el número de la que quieras.',
 NULL, TRUE, TRUE, @now, @now),

('f5000000-0000-0000-0000-000000000004', 'AGEND_SIN_HUECOS', 'No quedan huecos',
 'No queda ningún hueco para ese día. Dime otra fecha y lo miramos.',
 NULL, TRUE, TRUE, @now, @now),

('f5000000-0000-0000-0000-000000000005', 'AGEND_PIDE_NOMBRE', 'Pregunta por el nombre',
 '¿A nombre de quién la reservo?', NULL, TRUE, TRUE, @now, @now),

('f5000000-0000-0000-0000-000000000006', 'AGEND_PIDE_CODIGO', 'Pide el código de verificación',
 'Te envié un código de 6 dígitos. Escríbelo aquí para confirmar que este número es tuyo.',
 NULL, TRUE, TRUE, @now, @now),

('f5000000-0000-0000-0000-000000000007', 'AGEND_CONFIRMADA', 'Cita confirmada',
 'Listo. Te esperamos el {{FECHA}} a las {{HORA}} en {{NEGOCIO}}. Tu código es {{CODIGO}}; con él puedes consultarla o cancelarla.',
 NULL, TRUE, TRUE, @now, @now),

('f5000000-0000-0000-0000-000000000008', 'AGEND_SLOT_OCUPADO', 'El hueco se ocupó',
 'Justo se acaba de ocupar esa hora. Estas son las que quedan libres ahora.',
 'Sale cuando alguien reserva el mismo hueco mientras el cliente llenaba el formulario.', TRUE, TRUE, @now, @now),

('f5000000-0000-0000-0000-000000000009', 'AGEND_FUERA_DE_PLAZO', 'Cancelación fuera de plazo',
 'Ya no se puede cancelar por aquí: quedan menos de {{HORAS}} horas. Llama al negocio y lo arreglan contigo.',
 NULL, TRUE, TRUE, @now, @now),

('f5000000-0000-0000-0000-00000000000a', 'AGEND_NEGOCIO_CERRADO', 'Negocio sin agenda',
 'Este negocio todavía no tiene la agenda disponible. Escríbeles directamente y te atienden.',
 'Se muestra cuando el negocio no tiene empleados, servicios u horario configurados.', TRUE, TRUE, @now, @now);

-- ----- Permiso del registro retroactivo -----
-- Reutiliza el mecanismo que ya existe: `permission` + `role_permission`.
-- El control lo exige por codigo; no hay una segunda tabla de permisos.
INSERT INTO permission (Id, Code, Name, Description, Enabled, Visible, AuditDate, CreatedDate) VALUES
('22222222-0000-0000-0000-000000000101', 'APPOINTMENT_BACKDATE', 'Registrar cita ya realizada',
 'Permite anotar en la agenda un servicio que ya se prestó, aunque se solape con otra cita.',
 TRUE, TRUE, @now, @now);

-- Y se le asigna a quien debe tenerlo. Un permiso creado y no asignado a nadie
-- es una funcionalidad apagada: el boton no aparece y el endpoint la rechaza,
-- sin que nada en la pantalla explique por que.
--
-- OWNER y ADMIN si; EMPLOYEE no: registrar a mano un servicio en una franja ya
-- ocupada es justo la operacion que hay que poder auditar contra una persona
-- concreta, y el empleado ya tiene su propia agenda para lo que le toca.
INSERT INTO role_permission (Id, RoleId, PermissionId, Enabled, Visible, AuditDate, CreatedDate) VALUES
('33333333-0000-0000-0000-0000000000c1', '11111111-0000-0000-0000-000000000001',
 '22222222-0000-0000-0000-000000000101', TRUE, TRUE, @now, @now),
('33333333-0000-0000-0000-0000000000c2', '11111111-0000-0000-0000-000000000004',
 '22222222-0000-0000-0000-000000000101', TRUE, TRUE, @now, @now);

-- ===================================================================
-- Menu del directorio publico (ver scripts/sql/menu-directorio.sql).
-- ===================================================================

INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder,
                  IsSubmenu, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), 'TENANT_DIRECTORY', 'Directorio público', 'store',
       -- NIVEL SUPERIOR: el dock de "Mi empresa" solo pinta submenus cuya
       -- ruta sea un panel DENTRO de esa pantalla, y esto es una pagina propia.
       '/tenant/directorio', NULL, 6,
       0, TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM menu m WHERE m.Code = 'TENANT_DIRECTORY');

-- Solo el dueno: decidir si el negocio aparece en un directorio publico no es
-- una decision de un empleado.
INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), m.Id, r.Id, TRUE, TRUE, NOW(6), NOW(6)
FROM menu m
JOIN role r ON r.Code = 'OWNER'
WHERE m.Code = 'TENANT_DIRECTORY'
  AND NOT EXISTS (SELECT 1 FROM menu_role mr
                   WHERE mr.MenuId = m.Id AND mr.RoleId = r.Id);

-- ===================================================================
-- WhatsApp (ver scripts/sql/whatsapp*.sql).
-- ===================================================================

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
-- ---------------------------------------------------------------------------
-- Cada negocio con SU numero de WhatsApp.
--
-- Hasta ahora las credenciales eran una sola para toda la plataforma
-- (`saas.whatsapp.*`), asi que todos los avisos salian del mismo numero. Para
-- que un negocio reciba citas en el suyo hacen falta sus propias credenciales.
--
-- EL TOKEN VA CIFRADO. Un volcado de la base con los tokens en claro deja a
-- cualquiera enviando mensajes en nombre de todos los negocios conectados: es
-- de los pocos secretos aqui que valen por si solos. Se guarda con AES-GCM y la
-- llave vive en la configuracion del servicio, no en la base — separadas, que
-- es lo unico que hace que cifrar sirva de algo.
--
-- El numero legible (`WhatsappDisplayPhone`) y el nombre verificado se guardan
-- porque los devuelve Meta al conectar y son lo que la pantalla ENSENA. Volver
-- a preguntarselos a Meta en cada carga seria una llamada de red para pintar
-- una etiqueta.
-- ---------------------------------------------------------------------------

SET @hay := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'business_booking_policy'
               AND COLUMN_NAME = 'WhatsappAccessToken');
SET @ddl := IF(@hay = 0,
    'ALTER TABLE business_booking_policy
       ADD COLUMN WhatsappAccessToken VARCHAR(2000) NULL COMMENT ''Token de la Cloud API, CIFRADO con AES-GCM'' AFTER WhatsappPhoneId,
       ADD COLUMN WhatsappDisplayPhone VARCHAR(32) NULL COMMENT ''El numero tal y como se lee (+57 300 123 4567)'' AFTER WhatsappAccessToken,
       ADD COLUMN WhatsappVerifiedName VARCHAR(160) NULL COMMENT ''Nombre verificado que Meta muestra al destinatario'' AFTER WhatsappDisplayPhone,
       ADD COLUMN WhatsappVerifiedAt DATETIME(6) NULL COMMENT ''Cuando se comprobo contra Meta que las credenciales sirven'' AFTER WhatsappVerifiedName',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- ---------------------------------------------------------------------------
-- El webhook de CADA negocio.
--
-- Con credenciales pegadas, cada negocio tiene SU app de Meta, y Meta firma
-- cada entrega con el secreto de esa app. El webhook comprobaba la firma contra
-- un unico secreto de plataforma: cualquier negocio que conectara su numero
-- veria sus mensajes rechazados con 403 y nunca recibiria una cita.
--
-- El secreto de la app va CIFRADO por lo mismo que el token: con el se pueden
-- falsificar entregas hacia nosotros.
--
-- El verify token NO va cifrado a proposito: solo sirve para el apreton de
-- manos inicial, no autoriza nada, y hay que poder ENSENARSELO al dueno para
-- que lo pegue en Meta. Cifrar algo que se muestra en pantalla no protege de
-- nada y esconde que se puede mostrar.
-- ---------------------------------------------------------------------------

SET @hay := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'business_booking_policy'
               AND COLUMN_NAME = 'WhatsappAppSecret');
SET @ddl := IF(@hay = 0,
    'ALTER TABLE business_booking_policy
       ADD COLUMN WhatsappAppSecret VARCHAR(2000) NULL COMMENT ''Secreto de la app de Meta del negocio, CIFRADO'' AFTER WhatsappAccessToken,
       ADD COLUMN WhatsappVerifyToken VARCHAR(64) NULL COMMENT ''Palabra del apreton de manos del webhook. Se le ensena al dueno'' AFTER WhatsappAppSecret',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- ---------------------------------------------------------------------------
-- La entrada de menu de WhatsApp.
--
-- La pantalla existia y no habia forma de llegar a ella: una ruta sin entrada
-- de menu es una pantalla que solo encuentra quien escribe la URL a mano.
--
-- Va en el NIVEL SUPERIOR y no colgando de "Mi empresa": el dock de esa
-- pantalla solo pinta submenus cuya ruta sea un panel DENTRO de ella, y
-- WhatsApp es una pagina propia — colgada de ahi, la entrada existiria en la
-- base y no se veria en ninguna parte.
--
-- Se siembra por CODIGO y con los permisos del padre, no con ids escritos a
-- mano: asi una base que ya tenga la fila no la duplica, y el rol que ve "Mi
-- empresa" ve esto.
-- ---------------------------------------------------------------------------

-- Del que se copian los roles: quien administra su empresa administra esto.
SET @modelo := (SELECT Id FROM menu WHERE Code = 'TENANT_COMPANY' LIMIT 1);

INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder, IsSubmenu,
                  Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), 'TENANT_WHATSAPP', 'WhatsApp', 'message-circle', '/tenant/whatsapp',
       NULL, 7, FALSE, TRUE, TRUE, NOW(6), NOW(6)
WHERE @modelo IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT Id FROM menu WHERE Code = 'TENANT_WHATSAPP') t);

-- Lo ven los mismos roles que ven "Mi empresa": si no, la entrada existe y
-- nadie la tiene.
INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), nuevo.Id, mr.RoleId, TRUE, TRUE, NOW(6), NOW(6)
FROM (SELECT Id FROM menu WHERE Code = 'TENANT_WHATSAPP') nuevo
JOIN menu_role mr ON mr.MenuId = @modelo
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT MenuId, RoleId FROM menu_role) x
     WHERE x.MenuId = nuevo.Id AND x.RoleId = mr.RoleId);
-- ---------------------------------------------------------------------------
-- "Ingresos y egresos" en el menu.
--
-- La pantalla existe (/tenant/finanzas), el panel enlaza a ella y la caja del
-- negocio se lleva ahi — pero no tenia entrada de menu. Se llegaba solo desde
-- una tarjeta del panel, asi que quien no pasara por ahi no sabia que existia.
--
-- Ademas hace falta para que el guardian de rutas pueda cerrar lo que no se ve:
-- ese guardian compara contra el menu, y una pantalla legitima sin entrada
-- quedaria bloqueada.
-- ---------------------------------------------------------------------------

INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder,
                  IsSubmenu, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), 'TENANT_CASHBOOK', 'Ingresos y egresos', 'wallet',
       '/tenant/finanzas',
       (SELECT Id FROM (SELECT Id FROM menu WHERE Code = 'NEG') t),
       5, TRUE, TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM (SELECT Id FROM menu WHERE Code = 'TENANT_CASHBOOK') x);

INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), m.Id, r.Id, TRUE, TRUE, NOW(6), NOW(6)
FROM menu m JOIN role r ON r.Code = 'OWNER'
WHERE m.Code = 'TENANT_CASHBOOK'
  AND NOT EXISTS (SELECT 1 FROM (SELECT MenuId, RoleId FROM menu_role) x
                   WHERE x.MenuId = m.Id AND x.RoleId = r.Id);
-- =====================================================================
-- EL WEBHOOK SE DA DE ALTA SOLO
-- ---------------------------------------------------------------------
-- Hasta ahora el dueño, despues de pegar sus credenciales, tenia que
-- VOLVER a Meta y pegar alli dos valores mas (la URL y la palabra del
-- apreton de manos) y suscribirse al campo `messages`. Ese es el paso
-- que mas se olvida, y olvidarlo no falla: el negocio queda "conectado"
-- enviando y sin recibir una sola cita.
--
-- Meta tiene un endpoint para hacerlo por API:
--   POST /{WABA_ID}/subscribed_apps
--        { override_callback_uri, verify_token }
-- que con el token del propio dueño suscribe su app a su cuenta de
-- WhatsApp Y apunta el webhook aqui, de una vez.
--
-- Hace falta el identificador de la CUENTA de WhatsApp Business (el
-- WABA id), que Meta enseña en la misma pantalla y justo encima del
-- identificador del numero. Un campo mas al pegar, dos pasos menos
-- despues.
--
-- WhatsappWebhookAt guarda CUANDO se consiguio. Nulo significa que no
-- se pudo y que al dueño hay que seguir enseñandole las instrucciones a
-- mano: es la diferencia entre "no hace falta que hagas nada" y "haz
-- esto o no recibiras citas", y no se puede adivinar.
-- =====================================================================

SET @hay := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'business_booking_policy'
               AND COLUMN_NAME = 'WhatsappWabaId');
SET @ddl := IF(@hay = 0,
    'ALTER TABLE business_booking_policy
       ADD COLUMN WhatsappWabaId VARCHAR(40) NULL COMMENT ''Id de la cuenta de WhatsApp Business (WABA) del negocio'' AFTER WhatsappPhoneId,
       ADD COLUMN WhatsappWebhookAt DATETIME(6) NULL COMMENT ''Cuando se dio de alta el webhook contra Meta. NULL = hay que hacerlo a mano'' AFTER WhatsappVerifyToken',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
