-- =====================================================================
-- V10__1.0.0.sql
-- "Mi empresa": una sola entrada para las cinco configuraciones.
--
-- Mi negocio, Servicios, Sedes, Empleados y Mi pagina ocupaban cinco carriles
-- del menu lateral para algo que se toca de vez en cuando, y estorbaban la
-- navegacion diaria. Pasan a vivir dentro de "Mi empresa", que se elige en un
-- dock pegado al borde derecho de esa pantalla.
--
-- Las RUTAS sueltas siguen existiendo en el front (hay pasos del tour, enlaces
-- guardados y pruebas E2E que entran directo); lo que cambia es que ya no se
-- pintan en el menu. Por eso las filas NO se borran: se marcan Enabled=0, que
-- es justo lo que filtra la consulta de menus por rol (JpaMenuRepository:
-- m.enabled = true), asi dejan de listarse sin romper tour_step.MenuId.
--
-- NOTA: una variable de sesion vale para GUARDAR un valor a insertar, pero NO
-- para compararla contra una columna: el esquema es utf8mb4_unicode_ci y las
-- variables llegan como utf8mb4_0900_ai_ci, y MySQL corta con "Illegal mix of
-- collations" (1267). Las comparaciones van con literales, como en V8/V9.
-- =====================================================================

SET @now = NOW(6);

-- El id del grupo "Negocio" se genera en el arranque, asi que se resuelve por
-- codigo: fijarlo aqui romperia en cualquier base distinta a esta.
SET @grupo_negocio = (SELECT Id FROM menu WHERE Code = 'NEG' LIMIT 1);

-- ---------------------------------------------------------------------
-- La entrada nueva, dentro del grupo "Negocio" y en primer lugar.
-- ---------------------------------------------------------------------
INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT '77770002-0000-0000-0000-000000000030', 'TENANT_COMPANY', 'Mi empresa', 'building-2', '/tenant/mi-empresa',
       @grupo_negocio, 2, TRUE, TRUE, NULL, @now, @now
WHERE @grupo_negocio IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM menu m WHERE m.Code = 'TENANT_COMPANY');

INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000004', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE m.Code = 'TENANT_COMPANY'
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr
    WHERE mr.MenuId = m.Id AND mr.RoleId = '11111111-0000-0000-0000-000000000004'
);

-- ---------------------------------------------------------------------
-- Las cinco salen del menu (siguen existiendo como filas y como rutas).
-- ---------------------------------------------------------------------
UPDATE menu
SET Enabled = FALSE, AuditDate = @now
WHERE Code IN ('TENANT_BUSINESS', 'TENANT_SERVICES', 'TENANT_BRANCHES', 'TENANT_EMPLOYEES', 'TENANT_PAGE');

-- Compensaciones y Liquidaciones se quedan donde estaban: no son configuracion,
-- son la operacion del dia a dia. Suben de puesto al liberarse los carriles.
UPDATE menu SET DisplayOrder = 3, AuditDate = @now WHERE Code = 'TENANT_FINANCE';
UPDATE menu SET DisplayOrder = 4, AuditDate = @now WHERE Code = 'TENANT_SETTLEMENTS';
