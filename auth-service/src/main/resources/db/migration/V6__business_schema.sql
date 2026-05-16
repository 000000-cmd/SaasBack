CREATE TABLE business_type (
                                Id           CHAR(36)     NOT NULL,
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
                               AuditUser    CHAR(36)     NULL,
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
                               AuditUser    CHAR(36)     NULL,
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
                              AuditUser    CHAR(36)     NULL,
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
                              AuditUser    CHAR(36)     NULL,
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
