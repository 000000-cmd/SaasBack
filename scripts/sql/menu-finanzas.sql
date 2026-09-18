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
