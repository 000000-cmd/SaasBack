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