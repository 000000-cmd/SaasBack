-- ===========================================
-- Script de Inicialización Base de Datos
-- ===========================================

CREATE DATABASE IF NOT EXISTS saas_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE saas_db;

-- ===========================================
-- Configuraciones de MySQL para Optimización
-- ===========================================
SET GLOBAL max_connections = 150;
SET GLOBAL innodb_buffer_pool_size = 536870912; -- 512MB

-- ===========================================
-- Usuario adicional (opcional)
-- ===========================================
-- CREATE USER IF NOT EXISTS 'saas_user'@'%' IDENTIFIED BY 'saas_password';
-- GRANT ALL PRIVILEGES ON saas_db.* TO 'saas_user'@'%';
-- FLUSH PRIVILEGES;

-- ===========================================
-- Tablas base
-- ===========================================

SELECT 'Database initialized successfully' AS message;