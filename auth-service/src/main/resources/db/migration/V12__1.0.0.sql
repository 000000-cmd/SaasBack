-- =====================================================================
-- V12 — El menu sabe si es submenu.
--
-- Hasta ahora "que estas seis cosas se dibujen en el dock de Mi empresa"
-- estaba cableado en el front. Con esta bandera lo decide el administrador:
-- marcar IsSubmenu saca al menu del carril lateral y lo pinta en el nav
-- secundario de su padre. Por eso el padre pasa a ser obligatorio para un
-- submenu (lo valida MenuService: sin padre no hay donde pintarlo).
--
-- Las seis vuelven a Enabled = TRUE: ya no hace falta apagarlas para que
-- desaparezcan del sidebar, ahora las filtra la bandera. Apagarlas las
-- habria escondido tambien del dock.
--
-- Sin variables de sesion en las comparaciones: llegan con collation
-- utf8mb4_0900_ai_ci y las columnas son utf8mb4_unicode_ci (error 1267).
-- =====================================================================

SET @now = NOW(6);

-- ---------------------------------------------------------------------
-- 1) La columna. DEFAULT FALSE: todo lo que ya existe sigue siendo menu
--    normal, que es lo que era ayer.
-- ---------------------------------------------------------------------
ALTER TABLE menu
    ADD COLUMN IsSubmenu BIT(1) NOT NULL DEFAULT b'0' AFTER DisplayOrder;

-- ---------------------------------------------------------------------
-- 2) Las seis funcionalidades de configuracion cuelgan de "Mi empresa"
--    y pasan a ser submenus.
-- ---------------------------------------------------------------------
SET @mi_empresa = (SELECT Id FROM menu WHERE Code = 'TENANT_COMPANY' LIMIT 1);

UPDATE menu
SET IsSubmenu   = b'1',
    ParentId    = @mi_empresa,
    Enabled     = TRUE,
    AuditDate   = @now
WHERE @mi_empresa IS NOT NULL
  AND Code IN ('TENANT_BUSINESS', 'TENANT_SERVICES', 'TENANT_BRANCHES',
               'TENANT_EMPLOYEES', 'TENANT_PAGE', 'TENANT_FINANCE');

-- Orden dentro del dock: el mismo recorrido que sigue quien configura.
UPDATE menu SET DisplayOrder = 1, AuditDate = @now WHERE Code = 'TENANT_BUSINESS';
UPDATE menu SET DisplayOrder = 2, AuditDate = @now WHERE Code = 'TENANT_SERVICES';
UPDATE menu SET DisplayOrder = 3, AuditDate = @now WHERE Code = 'TENANT_BRANCHES';
UPDATE menu SET DisplayOrder = 4, AuditDate = @now WHERE Code = 'TENANT_EMPLOYEES';
UPDATE menu SET DisplayOrder = 5, AuditDate = @now WHERE Code = 'TENANT_PAGE';
UPDATE menu SET DisplayOrder = 6, AuditDate = @now WHERE Code = 'TENANT_FINANCE';
