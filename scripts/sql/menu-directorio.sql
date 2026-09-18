-- ---------------------------------------------------------------------------
-- La entrada de menu del directorio publico.
--
-- La ruta ya existia y no habia forma de llegar a ella. El menu del panel sale
-- de la BASE, no del codigo (por eso el administrador puede encender, apagar y
-- reordenar sin tocar nada), asi que una pantalla sin su fila aqui es una
-- pantalla que solo encuentra quien escriba la URL.
--
-- Cuelga de "Mi empresa" y no de la raiz: publicarse es configuracion del
-- negocio, no una operacion del dia a dia como la agenda.
--
-- Se siembra por CODIGO, no por un id escrito a mano: un id que ya sea de otra
-- cosa hace que el INSERT IGNORE se salte la fila EN SILENCIO.
-- ---------------------------------------------------------------------------

USE saas_db;

INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder,
                  IsSubmenu, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), 'TENANT_DIRECTORY', 'Directorio público', 'store',
       -- NIVEL SUPERIOR, no colgando de "Mi empresa": el dock de esa pantalla
       -- solo pinta submenus cuya ruta sea un panel DENTRO de ella, y esto es
       -- una pagina propia. Colgado de ahi, la entrada existia en la base y no
       -- se veia en ninguna parte.
       '/tenant/directorio', NULL, 6,
       0, TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM menu m WHERE m.Code = 'TENANT_DIRECTORY');

-- Solo el dueno: decidir si el negocio aparece en un directorio publico no es
-- una decision de un empleado.
INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), m.Id, r.Id, TRUE, TRUE, NOW(6), NOW(6)
FROM menu m
JOIN role r ON r.Code = 'OWNER'
WHERE m.Code = 'TENANT_DIRECTORY'
  AND NOT EXISTS (SELECT 1 FROM menu_role mr
                   WHERE mr.MenuId = m.Id AND mr.RoleId = r.Id);
