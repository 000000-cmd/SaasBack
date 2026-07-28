-- =====================================================================
-- V8__1.0.0.sql
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
-- =====================================================================

SET @now = NOW(6);

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

-- ---------------------------------------------------------------------
-- Menu de administracion: Tour guiado (ADMIN = 11111111-...-0001)
-- ---------------------------------------------------------------------
INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT '77770002-0000-0000-0000-000000000027', 'ADMIN_TOUR', 'Tour guiado', 'compass', '/admin/tour',
       '77770001-0000-0000-0000-000000000020', 7, TRUE, TRUE, NULL, @now, @now
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE Id = '77770002-0000-0000-0000-000000000027');

INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE m.Id = '77770002-0000-0000-0000-000000000027'
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr
    WHERE mr.MenuId = m.Id AND mr.RoleId = '11111111-0000-0000-0000-000000000001'
);

-- ---------------------------------------------------------------------
-- Recorrido inicial. Sin esto el tour arranca vacio en una instalacion nueva.
-- Los pasos 4 y 5 se resuelven por SELECT sobre menu: si ese menu no existe en
-- el entorno, la fila no se inserta y el recorrido sigue teniendo sentido.
-- ---------------------------------------------------------------------
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
('77772001-0000-0000-0000-000000000006', NULL, NULL,
 '¿Empezamos?',
 'Estos son los pasos mínimos para operar. Hazlos ahora o más tarde, a tu ritmo.',
 'circle-check', 6, TRUE, TRUE, NULL, @now, @now);

INSERT INTO tour_step (Id, MenuId, Anchor, Title, Body, Icon, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT '77772001-0000-0000-0000-000000000004', m.Id, NULL,
       'Tu equipo',
       'Das de alta a cada persona y ellos entran por la app móvil con su correo y contraseña.',
       'users', 4, TRUE, TRUE, NULL, @now, @now
FROM menu m WHERE m.Code = 'TENANT_EMPLOYEES'
  AND NOT EXISTS (SELECT 1 FROM tour_step t WHERE t.Id = '77772001-0000-0000-0000-000000000004');

INSERT INTO tour_step (Id, MenuId, Anchor, Title, Body, Icon, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT '77772001-0000-0000-0000-000000000005', m.Id, NULL,
       'Pagarle a tu equipo',
       'Aquí ves lo que le debes a cada empleado y confirmas su liquidación.',
       'banknote', 5, TRUE, TRUE, NULL, @now, @now
FROM menu m WHERE m.Code = 'TENANT_SETTLEMENTS'
  AND NOT EXISTS (SELECT 1 FROM tour_step t WHERE t.Id = '77772001-0000-0000-0000-000000000005');
