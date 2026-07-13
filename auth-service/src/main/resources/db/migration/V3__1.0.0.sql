-- =====================================================================
-- V3__1.0.0.sql
-- 1) business_landing: contenido de la página pública del negocio
--    (accedida por subdominio/slug) editable por el dueño con preview.
-- 2) Alta mínima de empleado: tercero y empleado nacen como "shells"
--    (solo FKs); el empleado completa sus datos desde el APK. Se relajan
--    las columnas NOT NULL correspondientes.
-- 3) Menús OWNER: "Mi página" (editor de la landing) y "Compensaciones"
--    (configuración financiera en cascada empresa→sede→empleado).
--    Se apaga "Crear mi negocio" (el onboarding ahora es gate de 1ª vez).
-- =====================================================================

SET @now = NOW(6);

-- ---------------------------------------------------------------------
-- 1) LANDING PÚBLICA DEL NEGOCIO (1:1 con business)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS business_landing;

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
    -- URLs de la galería como arreglo JSON (["url1","url2",...])
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

-- ---------------------------------------------------------------------
-- 2) SHELLS DE EMPLEADO: el alta mínima crea tercero/empleado solo con
--    sus FKs (userId/businessId y thirdPartyId/branchId). El resto lo
--    completa el propio empleado desde el APK.
-- ---------------------------------------------------------------------
ALTER TABLE third_party
    MODIFY COLUMN DocumentTypeId CHAR(36)    NULL,
    MODIFY COLUMN DocumentNumber VARCHAR(40) NULL;

-- La cuenta del empleado también nace mínima (usuario/correo/contraseña):
-- los nombres llegan cuando el empleado completa su perfil desde el APK.
ALTER TABLE app_user
    MODIFY COLUMN FirstName VARCHAR(80) NULL,
    MODIFY COLUMN LastName  VARCHAR(80) NULL;

ALTER TABLE employee
    MODIFY COLUMN PositionId CHAR(36) NULL,
    MODIFY COLUMN HireDate   DATE     NULL;

-- ---------------------------------------------------------------------
-- 3) MENÚS DEL DUEÑO (OWNER = 11111111-0000-0000-0000-000000000004)
-- ---------------------------------------------------------------------
INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT '77771001-0000-0000-0000-000000000010', 'TENANT_PAGE', 'Mi página', 'globe', '/tenant/mi-pagina', NULL, 6, TRUE, TRUE, NULL, @now, @now
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE Id = '77771001-0000-0000-0000-000000000010');

INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT '77771001-0000-0000-0000-000000000011', 'TENANT_FINANCE', 'Compensaciones', 'wallet', '/tenant/compensaciones', NULL, 7, TRUE, TRUE, NULL, @now, @now
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE Id = '77771001-0000-0000-0000-000000000011');

INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000004', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE m.Id IN ('77771001-0000-0000-0000-000000000010', '77771001-0000-0000-0000-000000000011')
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr
    WHERE mr.MenuId = m.Id
      AND mr.RoleId = '11111111-0000-0000-0000-000000000004'
);

-- "Crear mi negocio" deja de ser una opción del menú: el onboarding es un
-- gate de primera vez (la ruta redirige si el negocio ya existe).
UPDATE menu SET Enabled = FALSE, Visible = FALSE, AuditDate = @now
WHERE Id = '77771001-0000-0000-0000-000000000004';
