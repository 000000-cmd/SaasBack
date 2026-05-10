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
    Enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible         BOOLEAN      NOT NULL DEFAULT TRUE,
    AuditUser       CHAR(36)     NULL,
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
    AuditUser   CHAR(36)    NULL,
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
    AuditUser   CHAR(36)     NULL,
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
    AuditUser   CHAR(36)     NULL,
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
    AuditUser   CHAR(36)     NULL,
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
    AuditUser    CHAR(36)    NULL,
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
    AuditUser    CHAR(36)     NULL,
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
    AuditUser   CHAR(36)    NULL,
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
    AuditUser   CHAR(36)     NULL,
    AuditDate   DATETIME(6)  NOT NULL,
    CreatedDate DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_system_list_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE system_list_item (
    Id           CHAR(36)     NOT NULL,
    ListId       CHAR(36)     NOT NULL,
    Code         VARCHAR(80)  NOT NULL,
    Name         VARCHAR(120) NOT NULL,
    Value        VARCHAR(500) NULL,
    DisplayOrder INT          NOT NULL DEFAULT 0,
    Enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    Visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    AuditUser    CHAR(36)     NULL,
    AuditDate    DATETIME(6)  NOT NULL,
    CreatedDate  DATETIME(6)  NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_system_list_item_code (ListId, Code),
    KEY idx_system_list_item_list (ListId),
    CONSTRAINT fk_system_list_item_list
        FOREIGN KEY (ListId) REFERENCES system_list (Id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE constant (
    Id          CHAR(36)      NOT NULL,
    Code        VARCHAR(80)   NOT NULL,
    Name        VARCHAR(120)  NOT NULL,
    Value       VARCHAR(1000) NOT NULL,
    Description VARCHAR(500)  NULL,
    Enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
    Visible     BOOLEAN       NOT NULL DEFAULT TRUE,
    AuditUser   CHAR(36)      NULL,
    AuditDate   DATETIME(6)   NOT NULL,
    CreatedDate DATETIME(6)   NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY uq_constant_code (Code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
