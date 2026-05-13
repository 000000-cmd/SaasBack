-- =====================================================================
-- cleanup-tenant-menus.sql
-- Script manual (NO es una migracion Flyway). Ejecutar una vez en DBs de
-- dev que ya tenian los menus tenant insertados por la version antigua de
-- V3__realign_menus.sql.
--
-- Uso:
--   mysql -u <user> -p <database> < scripts/cleanup-tenant-menus.sql
-- =====================================================================

DELETE FROM menu_role
WHERE MenuId IN (
    SELECT Id FROM menu
    WHERE Code LIKE 'TENANT_%'
       OR Id LIKE '77770001-1000-%'
       OR Id LIKE '77770002-1000-%'
);

DELETE FROM menu
WHERE Code LIKE 'TENANT_%'
   OR Id LIKE '77770001-1000-%'
   OR Id LIKE '77770002-1000-%';
