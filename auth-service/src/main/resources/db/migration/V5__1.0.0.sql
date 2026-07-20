-- =====================================================================
-- V5__1.0.0.sql
-- Catalogo de ESPECIALIDADES per-business (espejo de offering_category) y
-- FK de especialidad en servicios (business_offering) y empleados (employee).
--
-- La especialidad agrupa servicios por disciplina (Barberia, Estilismo,
-- Manicure...) y clasifica al empleado. Habilita la simulacion de compensacion
-- por % de servicio filtrada a la especialidad del empleado (Fase B).
--
-- SpecialtyId es nullable y SIN FK dura (igual que business_offering.CategoryId):
-- no rompe filas existentes y evita acoplar el orden de inserciones.
-- =====================================================================

CREATE TABLE specialty (
    Id CHAR(36) NOT NULL,
    BusinessId CHAR(36) NOT NULL,
    Name VARCHAR(120) NOT NULL,
    DisplayOrder INT NOT NULL DEFAULT 0,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    KEY idx_specialty_business (BusinessId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE business_offering ADD COLUMN SpecialtyId CHAR(36) NULL;
ALTER TABLE employee ADD COLUMN SpecialtyId CHAR(36) NULL;
