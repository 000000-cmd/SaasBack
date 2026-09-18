-- ---------------------------------------------------------------------------
-- La entrada de menu de WhatsApp.
--
-- La pantalla existia y no habia forma de llegar a ella: una ruta sin entrada
-- de menu es una pantalla que solo encuentra quien escribe la URL a mano.
--
-- Va en el NIVEL SUPERIOR y no colgando de "Mi empresa": el dock de esa
-- pantalla solo pinta submenus cuya ruta sea un panel DENTRO de ella, y
-- WhatsApp es una pagina propia — colgada de ahi, la entrada existiria en la
-- base y no se veria en ninguna parte.
--
-- Se siembra por CODIGO y con los permisos del padre, no con ids escritos a
-- mano: asi una base que ya tenga la fila no la duplica, y el rol que ve "Mi
-- empresa" ve esto.
-- ---------------------------------------------------------------------------

USE saas_db;

-- Del que se copian los roles: quien administra su empresa administra esto.
SET @modelo := (SELECT Id FROM menu WHERE Code = 'TENANT_COMPANY' LIMIT 1);

INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder, IsSubmenu,
                  Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), 'TENANT_WHATSAPP', 'WhatsApp', 'message-circle', '/tenant/whatsapp',
       NULL, 7, FALSE, TRUE, TRUE, NOW(6), NOW(6)
WHERE @modelo IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT Id FROM menu WHERE Code = 'TENANT_WHATSAPP') t);

-- Lo ven los mismos roles que ven "Mi empresa": si no, la entrada existe y
-- nadie la tiene.
INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), nuevo.Id, mr.RoleId, TRUE, TRUE, NOW(6), NOW(6)
FROM (SELECT Id FROM menu WHERE Code = 'TENANT_WHATSAPP') nuevo
JOIN menu_role mr ON mr.MenuId = @modelo
WHERE NOT EXISTS (
    SELECT 1 FROM (SELECT MenuId, RoleId FROM menu_role) x
     WHERE x.MenuId = nuevo.Id AND x.RoleId = mr.RoleId);
