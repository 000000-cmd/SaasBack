-- =====================================================================
-- V3__realign_menus.sql (CORREGIDO)
-- Idempotente: no falla si ya se ejecutó antes
-- Evita duplicados y errores por FK
-- =====================================================================

SET @now = NOW(6);

-- ---------------------------------------------------------------------
-- 1. MENUS (UPSERT SEGURO)
-- ---------------------------------------------------------------------
INSERT INTO menu (
    Id, Code, Name, Icon, Route, ParentId,
    DisplayOrder, Enabled, Visible,
    AuditUser, AuditDate, CreatedDate
) VALUES

-- ================= ADMIN =================

('77770001-0000-0000-0000-000000000001', 'ADMIN_DASHBOARD', 'Panel', 'activity', '/admin/dashboard', NULL, 1, TRUE, TRUE, NULL, @now, @now),

('77770001-0000-0000-0000-000000000010', 'ADMIN_USERS_GROUP', 'Usuarios', 'users', NULL, NULL, 2, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000011', 'ADMIN_USERS', 'Todos los usuarios', 'users', '/admin/users', '77770001-0000-0000-0000-000000000010', 1, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000012', 'ADMIN_INVITES', 'Invitaciones', 'user-plus', '/admin/invitations', '77770001-0000-0000-0000-000000000010', 2, TRUE, TRUE, NULL, @now, @now),

('77770001-0000-0000-0000-000000000020', 'ADMIN_SYSTEM_GROUP', 'Sistema', 'settings', NULL, NULL, 3, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000021', 'ADMIN_LISTS', 'Listas', 'list-tree', '/admin/system-lists', '77770001-0000-0000-0000-000000000020', 1, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000022', 'ADMIN_CONSTANTS', 'Constantes', 'hash', '/admin/constants', '77770001-0000-0000-0000-000000000020', 2, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000023', 'ADMIN_MENUS', 'Menus', 'menu', '/admin/menus', '77770001-0000-0000-0000-000000000020', 3, TRUE, TRUE, NULL, @now, @now),

('77770001-0000-0000-0000-000000000030', 'ADMIN_SECURITY_GROUP', 'Seguridad', 'shield', NULL, NULL, 4, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000031', 'ADMIN_ROLES', 'Roles', 'shield', '/admin/roles', '77770001-0000-0000-0000-000000000030', 1, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000032', 'ADMIN_PERMS', 'Permisos', 'shield-check', '/admin/permissions', '77770001-0000-0000-0000-000000000030', 2, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000033', 'ADMIN_AUDIT', 'Auditoria', 'scroll-text', '/admin/audit', '77770001-0000-0000-0000-000000000030', 3, TRUE, TRUE, NULL, @now, @now),

('77770001-0000-0000-0000-000000000040', 'ADMIN_PREFS_GROUP', 'Preferencias', 'settings', NULL, NULL, 9, TRUE, TRUE, NULL, @now, @now),
('77770002-0000-0000-0000-000000000041', 'ADMIN_PROFILE', 'Mi perfil', 'user', '/admin/profile', '77770001-0000-0000-0000-000000000040', 1, TRUE, TRUE, NULL, @now, @now),

-- ================= TENANT =================

('77770001-1000-0000-0000-000000000001', 'TENANT_OPS_GROUP', 'Operacion', 'activity', NULL, NULL, 1, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000002', 'TENANT_DASHBOARD', 'Panel', 'activity', '/tenant/dashboard', '77770001-1000-0000-0000-000000000001', 1, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000003', 'TENANT_AGENDA', 'Agenda del dia', 'clock', '/tenant/citas/agenda', '77770001-1000-0000-0000-000000000001', 2, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000004', 'TENANT_HISTORY', 'Historial', 'activity', '/tenant/citas/historial', '77770001-1000-0000-0000-000000000001', 3, TRUE, TRUE, NULL, @now, @now),

('77770001-1000-0000-0000-000000000010', 'TENANT_TEAM_GROUP', 'Equipo', 'users', NULL, NULL, 2, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000011', 'TENANT_EMPLOYEES', 'Empleados', 'users', '/tenant/empleados', '77770001-1000-0000-0000-000000000010', 1, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000012', 'TENANT_SHIFTS', 'Turnos', 'clock', '/tenant/empleados/turnos', '77770001-1000-0000-0000-000000000010', 2, TRUE, TRUE, NULL, @now, @now),

('77770001-1000-0000-0000-000000000020', 'TENANT_BIZ_GROUP', 'Negocio', 'package', NULL, NULL, 3, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000021', 'TENANT_PRODUCTS', 'Productos', 'package', '/tenant/inventario/productos', '77770001-1000-0000-0000-000000000020', 1, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000022', 'TENANT_STOCK', 'Alertas stock', 'alert-triangle', '/tenant/inventario/alertas', '77770001-1000-0000-0000-000000000020', 2, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000023', 'TENANT_PAYROLL', 'Nomina', 'wallet', '/tenant/nomina', '77770001-1000-0000-0000-000000000020', 3, TRUE, TRUE, NULL, @now, @now),

('77770001-1000-0000-0000-000000000030', 'TENANT_CFG_GROUP', 'Configuracion', 'settings', NULL, NULL, 4, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000031', 'TENANT_CFG_BIZ', 'Ajustes negocio', 'settings', '/tenant/configuracion', '77770001-1000-0000-0000-000000000030', 1, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000032', 'TENANT_SERVICES', 'Tipos de servicio', 'scissors', '/tenant/configuracion/servicios', '77770001-1000-0000-0000-000000000030', 2, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000033', 'TENANT_DEDUCTIONS', 'Deducciones', 'minus-circle', '/tenant/configuracion/deducciones', '77770001-1000-0000-0000-000000000030', 3, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000034', 'TENANT_INTEGRATIONS', 'Integraciones', 'message-circle', '/tenant/configuracion/integraciones', '77770001-1000-0000-0000-000000000030', 4, TRUE, TRUE, NULL, @now, @now),
('77770002-1000-0000-0000-000000000035', 'TENANT_PROFILE', 'Mi perfil', 'user', '/tenant/profile', '77770001-1000-0000-0000-000000000030', 9, TRUE, TRUE, NULL, @now, @now)

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
-- 2. MENU_ROLE (SIN DUPLICAR)
-- ---------------------------------------------------------------------

-- ADMIN
INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE (m.Id LIKE '77770001-0000-%' OR m.Id LIKE '77770002-0000-%')
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr
    WHERE mr.MenuId = m.Id
      AND mr.RoleId = '11111111-0000-0000-0000-000000000001'
);

-- USER
INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000002', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE (m.Id LIKE '77770001-1000-%' OR m.Id LIKE '77770002-1000-%')
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr
    WHERE mr.MenuId = m.Id
      AND mr.RoleId = '11111111-0000-0000-0000-000000000002'
);
