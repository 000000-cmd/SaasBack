-- =====================================================================
-- V9__1.0.0.sql
-- Gestion de Elasticsearch.
--
-- El reindex dejo de ser una accion escondida dentro de la pantalla de
-- terceros: ahora es una funcionalidad propia que cubre TODOS los indices
-- (usuarios, roles, localizacion, terceros, saldos) con tres granularidades
-- (un registro / una entidad / todo). El permiso viejo THIRDPARTY_REINDEX se
-- queda por compatibilidad, pero la pantalla nueva pide ELASTIC_MANAGE.
-- =====================================================================

SET @now = NOW(6);

-- ---------------------------------------------------------------------
-- Permiso
-- ---------------------------------------------------------------------
INSERT INTO permission (Id, Code, Name, Description, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT '22222222-0000-0000-0000-000000000008', 'ELASTIC_MANAGE', 'Gestionar Elasticsearch',
       'Permite consultar el estado de los indices y reindexar registros, entidades o todo', TRUE, TRUE, NULL, @now, @now
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE Code = 'ELASTIC_MANAGE');

INSERT INTO role_permission (Id, RoleId, PermissionId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT '33333333-0000-0000-0000-0000000000a8', '11111111-0000-0000-0000-000000000001', p.Id, TRUE, TRUE, NULL, @now, @now
FROM permission p
WHERE p.Code = 'ELASTIC_MANAGE'
  AND NOT EXISTS (
    SELECT 1 FROM role_permission rp
    WHERE rp.RoleId = '11111111-0000-0000-0000-000000000001' AND rp.PermissionId = p.Id
);

-- ---------------------------------------------------------------------
-- Menu de administracion (ADMIN = 11111111-...-0001, padre = grupo sistema)
-- ---------------------------------------------------------------------
INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT '77770002-0000-0000-0000-000000000028', 'ADMIN_ELASTIC', 'Gestion de Elastic', 'database', '/admin/elastic',
       '77770001-0000-0000-0000-000000000020', 8, TRUE, TRUE, NULL, @now, @now
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE Id = '77770002-0000-0000-0000-000000000028');

INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000001', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE m.Id = '77770002-0000-0000-0000-000000000028'
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr
    WHERE mr.MenuId = m.Id AND mr.RoleId = '11111111-0000-0000-0000-000000000001'
);
