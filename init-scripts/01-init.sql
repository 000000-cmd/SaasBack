-- ============================================================
-- Inicializacion minima del contenedor MySQL
-- Solo crea la BD vacia. El esquema y datos son gestionados
-- por Flyway (auth-service/db/migration/Vx__*.sql) al arranque.
-- ============================================================

CREATE DATABASE IF NOT EXISTS saas_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

SET GLOBAL max_connections = 200;
