
-- ============================================================
-- TABLE: third_parties
-- ============================================================

CREATE TABLE third_parties (
   Id CHAR(36) NOT NULL,

   Enabled BOOLEAN NOT NULL DEFAULT TRUE,
   Visible BOOLEAN NOT NULL DEFAULT TRUE,

   AuditUser CHAR(36) NULL,
   AuditDate DATETIME NOT NULL,
   CreatedDate DATETIME NOT NULL,

   type VARCHAR(20) NOT NULL,

   document_type_id CHAR(36) NOT NULL,

   document_number VARCHAR(50) NOT NULL,

   user_id CHAR(36) NULL,

   first_name VARCHAR(100) NULL,
   second_name VARCHAR(100) NULL,

   first_last_name VARCHAR(100) NULL,
   second_last_name VARCHAR(100) NULL,

   email VARCHAR(255) NULL,

   phone VARCHAR(30) NULL,

   business_name VARCHAR(255) NULL,

   trade_name VARCHAR(255) NULL,

   active BOOLEAN NOT NULL DEFAULT TRUE,

   CONSTRAINT pk_third_parties
       PRIMARY KEY (Id),

   CONSTRAINT uq_third_party_document
       UNIQUE (document_type_id, document_number),

   CONSTRAINT fk_third_party_document_type
       FOREIGN KEY (document_type_id)
           REFERENCES document_type (Id),

   CONSTRAINT fk_third_party_user
       FOREIGN KEY (user_id)
           REFERENCES app_user (Id),
   CONSTRAINT chk_third_party_type
       CHECK (type IN ('PERSON','COMPANY'))

);
CREATE INDEX idx_third_party_document
    ON third_parties(document_number);

CREATE INDEX idx_third_party_user
    ON third_parties(user_id);

CREATE INDEX idx_third_party_email
    ON third_parties(email);

CREATE INDEX idx_third_party_business_name
    ON third_parties(business_name);

INSERT INTO third_parties (
    Id,
    Enabled,
    Visible,
    AuditUser,
    AuditDate,
    CreatedDate,
    type,
    document_type_id,
    document_number,
    user_id,
    first_name,
    second_name,
    first_last_name,
    second_last_name,
    email,
    phone,
    business_name,
    trade_name,
    active
)
VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        TRUE,
        TRUE,
        NULL,
        NOW(),
        NOW(),
        'PERSON',
        '55555555-0000-0000-0000-000000000001',
        '123456789',
        NULL,
        'Juan',
        'Carlos',
        'Pérez',
        'Rodríguez',
        'juan.perez@empresa.com',
        '3001234567',
        NULL,
        NULL,
        TRUE
    ),

    (
        '10000000-0000-0000-0000-000000000002',
        TRUE,
        TRUE,
        NULL,
        NOW(),
        NOW(),
        'PERSON',
        '55555555-0000-0000-0000-000000000001',
        '987654321',
        NULL,
        'María',
        'Fernanda',
        'Gómez',
        'López',
        'maria.gomez@empresa.com',
        '3019876543',
        NULL,
        NULL,
        TRUE
    ),

    (
        '10000000-0000-0000-0000-000000000003',
        TRUE,
        TRUE,
        NULL,
        NOW(),
        NOW(),
        'COMPANY',
        '55555555-0000-0000-0000-000000000001',
        '900123456',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        'contacto@acme.com',
        '6076543210',
        'ACME S.A.S.',
        'ACME',
        TRUE
    ),

    (
        '10000000-0000-0000-0000-000000000004',
        TRUE,
        TRUE,
        NULL,
        NOW(),
        NOW(),
        'COMPANY',
        '55555555-0000-0000-0000-000000000001',
        '901555888',
        NULL,
        NULL,
        NULL,
        NULL,
        NULL,
        'ventas@globex.com',
        '6041234567',
        'Globex Colombia S.A.S.',
        'Globex',
        TRUE
    );