-- =====================================================================
-- V11 — "Compensaciones" pasa a ser un panel de "Mi empresa".
--
-- Configurar cuanto gana cada quien se toca tan poco como el resto de la
-- configuracion, asi que se va al dock de Mi empresa y libera un carril del
-- menu lateral. Liquidaciones NO se mueve: eso si es operacion diaria.
--
-- La fila NO se borra: solo se apaga. JpaMenuRepository filtra por
-- m.enabled = true, asi que basta con eso para que desaparezca del sidebar, y
-- la ruta /tenant/compensaciones sigue viva (hay pasos de tour y specs E2E
-- que entran directo).
--
-- Sin variables de sesion en las comparaciones: llegan con collation
-- utf8mb4_0900_ai_ci y las columnas son utf8mb4_unicode_ci (error 1267).
-- =====================================================================

SET @now = NOW(6);

UPDATE menu SET Enabled = FALSE, AuditDate = @now WHERE Code = 'TENANT_FINANCE';

-- Liquidaciones sube al puesto que deja libre.
UPDATE menu SET DisplayOrder = 3, AuditDate = @now WHERE Code = 'TENANT_SETTLEMENTS';
