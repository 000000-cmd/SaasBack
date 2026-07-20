-- =====================================================================
-- V6__1.0.0.sql
-- Salario base para tipos de compensacion HIBRIDOS (salario + %).
--
-- El modelo tenia una sola cifra (CompensationValue) por nivel: para
-- "Salario + % servicio"/"Salario + comision" solo guardaba el %, sin el
-- salario base. SalaryBase (nullable) lo agrega SIN migrar datos: los tipos
-- no-hibridos lo dejan NULL y CompensationValue mantiene su significado.
-- =====================================================================

ALTER TABLE business_compensation ADD COLUMN SalaryBase DECIMAL(12,2) NULL;
ALTER TABLE branch_compensation   ADD COLUMN SalaryBase DECIMAL(12,2) NULL;
ALTER TABLE employee_compensation ADD COLUMN SalaryBase DECIMAL(12,2) NULL;
