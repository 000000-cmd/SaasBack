-- =====================================================================
-- V1__1.0.0.sql
-- Schema + seed unificado de la plataforma SaaS (version de app 1.0.0).
-- Consolida: V1__schema, V2__seed_base, V3__realign_menus,
-- V4__outbox_event, V6__business_schema y la tabla third_party.
-- El esquema de localizaciones vive aparte en V2__1.0.0.sql.
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

CREATE TABLE app_user (
    Id              CHAR(36)     NOT NULL,
    Username        VARCHAR(60)  NOT NULL,
    Email           VARCHAR(120) NOT NULL,
    PasswordHash    VARCHAR(120) NOT NULL,
    FirstName       VARCHAR(80)  NOT NULL,
    LastName        VARCHAR(80)  NOT NULL,
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

CREATE TABLE third_party (
    Id             CHAR(36)     NOT NULL,
    DocumentTypeId CHAR(36)     NOT NULL,
    DocumentNumber VARCHAR(40)  NOT NULL,
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
    CONSTRAINT fk_third_party_document_type FOREIGN KEY (DocumentTypeId) REFERENCES document_type (Id),
    CONSTRAINT fk_third_party_user FOREIGN KEY (UserId) REFERENCES app_user (Id),
    CONSTRAINT fk_third_party_gender FOREIGN KEY (GenderId) REFERENCES gender (Id)
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
    ('44444444-0000-0000-0000-000000000003', 'gender',          'Generos',                     'Catalogo de generos',                          TRUE, TRUE, NULL, @now, @now);

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

-- ---------------------------------------------------------------------
-- CONSTANTS (valores de configuracion globales, todos como STRING)
-- ---------------------------------------------------------------------
INSERT INTO constant (Id, Code, Name, Value, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('66666666-0000-0000-0000-000000000001', 'MAYORIA_EDAD',        'Mayoria de edad',                  '18',  'Edad minima para ser mayor de edad',                              TRUE, TRUE, NULL, @now, @now),
    ('66666666-0000-0000-0000-000000000002', 'MAX_LOGIN_ATTEMPTS',  'Maximo intentos de login',         '5',   'Cantidad maxima de intentos fallidos antes de bloquear cuenta',   TRUE, TRUE, NULL, @now, @now),
    ('66666666-0000-0000-0000-000000000003', 'SESION_TIMEOUT_MIN',  'Timeout de sesion (minutos)',      '60',  'Tiempo de inactividad antes de cerrar sesion automaticamente',    TRUE, TRUE, NULL, @now, @now),
    ('66666666-0000-0000-0000-000000000004', 'PROFILE_PHOTO_MAX_KB', 'Tamano maximo foto de perfil (KB)', '512', 'Limite de tamano para subir foto de perfil de usuario',           TRUE, TRUE, NULL, @now, @now),
    ('66666666-0000-0000-0000-000000000005', 'VERAPP',              'Version vigente del APK',          '1.0.0', 'Si la version instalada difiere, el APK exige actualizar',        TRUE, TRUE, NULL, @now, @now);

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
-- ---------------------------------------------------------------------
INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE (m.Id LIKE '77770001-0000-%' OR m.Id LIKE '77770002-0000-%')
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr
    WHERE mr.MenuId = m.Id
      AND mr.RoleId = '11111111-0000-0000-0000-000000000001'
);

-- ===================== [menus tenant - OWNER] =====================
-- Menus del dueño (rol OWNER). Apuntan a rutas /tenant/* (área separada de /admin).
-- El sidebar del dueño se nutre de estos via /menus/me (config por rol).
INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
('77771001-0000-0000-0000-000000000001', 'TENANT_DASHBOARD',  'Panel',            'activity',   '/tenant/dashboard',  NULL, 1, TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000002', 'TENANT_BUSINESS',   'Mi negocio',       'building-2', '/tenant/mi-negocio', NULL, 2, TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000005', 'TENANT_SERVICES',   'Servicios',        'scissors',   '/tenant/servicios',  NULL, 3, TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000006', 'TENANT_BRANCHES',   'Sedes',            'map-pin',    '/tenant/sedes',      NULL, 4, TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000007', 'TENANT_EMPLOYEES',  'Empleados',        'users',      '/tenant/empleados',  NULL, 5, TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000003', 'TENANT_PROFILE',    'Mi perfil',        'user',       '/tenant/profile',    NULL, 9, TRUE, TRUE, NULL, @now, @now),
('77771001-0000-0000-0000-000000000004', 'TENANT_ONBOARDING', 'Crear mi negocio', 'plus',       '/tenant/onboarding', NULL, 8, TRUE, TRUE, NULL, @now, @now);

INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000004', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE m.Id LIKE '77771001-0000-%'
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
INSERT INTO third_party
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

-- ----- Terceros: hijos -----
CREATE TABLE third_party_contact (
    Id CHAR(36) NOT NULL, ThirdPartyId CHAR(36) NOT NULL, ContactTypeId CHAR(36) NOT NULL,
    Value VARCHAR(160) NOT NULL, IsPrimary BOOLEAN NOT NULL DEFAULT FALSE, IsVerified BOOLEAN NOT NULL DEFAULT FALSE, VerifiedAt DATETIME(6) NULL, Notes VARCHAR(255) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_tpc_third_party (ThirdPartyId), KEY idx_tpc_contact_type (ContactTypeId),
    CONSTRAINT fk_tpc_third_party FOREIGN KEY (ThirdPartyId) REFERENCES third_party (Id),
    CONSTRAINT fk_tpc_contact_type FOREIGN KEY (ContactTypeId) REFERENCES contact_type (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE third_party_address (
    Id CHAR(36) NOT NULL, ThirdPartyId CHAR(36) NOT NULL, AddressTypeId CHAR(36) NULL,
    MunicipalityId CHAR(36) NOT NULL, NeighborhoodId CHAR(36) NULL, Line VARCHAR(255) NULL, Reference VARCHAR(255) NULL, IsPrimary BOOLEAN NOT NULL DEFAULT FALSE,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_tpa_third_party (ThirdPartyId), KEY idx_tpa_municipality (MunicipalityId), KEY idx_tpa_neighborhood (NeighborhoodId),
    CONSTRAINT fk_tpa_third_party FOREIGN KEY (ThirdPartyId) REFERENCES third_party (Id),
    CONSTRAINT fk_tpa_address_type FOREIGN KEY (AddressTypeId) REFERENCES address_type (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----- Empresa y derivados -----
CREATE TABLE business (
    Id CHAR(36) NOT NULL, BusinessTypeId CHAR(36) NOT NULL, Name VARCHAR(160) NOT NULL, LegalName VARCHAR(200) NULL, TradeName VARCHAR(160) NULL,
    DocumentTypeId CHAR(36) NULL, DocumentNumber VARCHAR(40) NULL, LogoUrl VARCHAR(500) NULL, StatusId CHAR(36) NULL,
    PrimaryColor VARCHAR(20) NULL, SecondaryColor VARCHAR(20) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    CONSTRAINT fk_business_type FOREIGN KEY (BusinessTypeId) REFERENCES business_type (Id),
    CONSTRAINT fk_business_doc_type FOREIGN KEY (DocumentTypeId) REFERENCES document_type (Id),
    CONSTRAINT fk_business_status FOREIGN KEY (StatusId) REFERENCES status (Id)
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
    MunicipalityId CHAR(36) NOT NULL, NeighborhoodId CHAR(36) NULL, AddressLine VARCHAR(255) NULL, Phone VARCHAR(30) NULL, IsMain BOOLEAN NOT NULL DEFAULT FALSE, StatusId CHAR(36) NULL,
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
    CONSTRAINT fk_bo_business FOREIGN KEY (BusinessId) REFERENCES business (Id),
    CONSTRAINT fk_bo_third_party FOREIGN KEY (ThirdPartyId) REFERENCES third_party (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE employee (
    Id CHAR(36) NOT NULL, ThirdPartyId CHAR(36) NOT NULL, BranchId CHAR(36) NOT NULL, PositionId CHAR(36) NOT NULL, EmployeeCode VARCHAR(40) NULL, HireDate DATE NOT NULL, TerminationDate DATE NULL, StatusId CHAR(36) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_emp_branch (BranchId), KEY idx_emp_third_party (ThirdPartyId),
    CONSTRAINT fk_emp_third_party FOREIGN KEY (ThirdPartyId) REFERENCES third_party (Id),
    CONSTRAINT fk_emp_branch FOREIGN KEY (BranchId) REFERENCES branch (Id),
    CONSTRAINT fk_emp_position FOREIGN KEY (PositionId) REFERENCES employee_position (Id),
    CONSTRAINT fk_emp_status FOREIGN KEY (StatusId) REFERENCES status (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE client (
    Id CHAR(36) NOT NULL, ThirdPartyId CHAR(36) NOT NULL, RegistrationStatusId CHAR(36) NULL, AcquisitionSource VARCHAR(80) NULL, Notes VARCHAR(255) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), UNIQUE KEY uq_client_third_party (ThirdPartyId),
    CONSTRAINT fk_client_third_party FOREIGN KEY (ThirdPartyId) REFERENCES third_party (Id),
    CONSTRAINT fk_client_reg_status FOREIGN KEY (RegistrationStatusId) REFERENCES registration_status (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----- Ofertas (servicios que ofrece la empresa) -----
CREATE TABLE offering_category (
    Id CHAR(36) NOT NULL, BusinessId CHAR(36) NOT NULL, Name VARCHAR(120) NOT NULL, DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_oc_business (BusinessId),
    CONSTRAINT fk_oc_business FOREIGN KEY (BusinessId) REFERENCES business (Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE business_offering (
    Id CHAR(36) NOT NULL, BusinessId CHAR(36) NOT NULL, CategoryId CHAR(36) NULL, Name VARCHAR(160) NOT NULL, Description VARCHAR(500) NULL, DurationMinutes INT NOT NULL, Price DECIMAL(12,2) NOT NULL, IsActive BOOLEAN NOT NULL DEFAULT TRUE,
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

CREATE TABLE employee_compensation (
    Id CHAR(36) NOT NULL, EmployeeId CHAR(36) NOT NULL, CompensationType VARCHAR(40) NOT NULL, BaseSalary DECIMAL(12,2) NULL, ServicePercentage DECIMAL(5,2) NULL, FixedCommission DECIMAL(12,2) NULL, ValidFrom DATETIME(6) NOT NULL, ValidTo DATETIME(6) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE, CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id), KEY idx_ec_employee (EmployeeId),
    CONSTRAINT fk_ec_employee FOREIGN KEY (EmployeeId) REFERENCES employee (Id)
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
-- FASE B: permiso de reindexado de terceros (asignado a ADMIN)
-- ---------------------------------------------------------------------
INSERT INTO permission (Id, Code, Name, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('22222222-0000-0000-0000-000000000007', 'THIRDPARTY_REINDEX', 'Reindexar tercero', 'Permite reindexar un tercero en Elasticsearch', TRUE, TRUE, NULL, @now, @now);
INSERT INTO role_permission (Id, RoleId, PermissionId, Enabled, Visible, AuditUser, AuditDate, CreatedDate) VALUES
    ('33333333-0000-0000-0000-0000000000a7', '11111111-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000007', TRUE, TRUE, NULL, @now, @now);
