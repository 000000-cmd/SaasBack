-- =====================================================================
-- V7__1.0.0.sql
-- Liquidacion de comisiones al empleado (auditoria de tesoreria).
--
-- Confirmar una liquidacion mueve dinero: suma a AmountPaid del saldo del
-- empleado (employee_balance, V4) y por tanto baja su Balance por cobrar. Es
-- IRREVERSIBLE, asi que cada confirmacion deja su fila aqui: quien liquido,
-- cuanto, cuando y sobre que saldo. Es el "historial de liquidaciones" que
-- consulta el dueno.
--
-- El desglose servicio a servicio (que servicios componen el monto) llegara
-- con el modulo de citas/servicios prestados; por eso NO hay tabla de detalle
-- todavia: se liquida contra el saldo por cobrar acumulado.
-- =====================================================================

CREATE TABLE employee_settlement (
    Id CHAR(36) NOT NULL,
    BusinessId CHAR(36) NOT NULL,
    BranchId CHAR(36) NULL,
    EmployeeId CHAR(36) NOT NULL,
    -- Monto liquidado en esta operacion (siempre > 0).
    Amount DECIMAL(14,2) NOT NULL,
    -- Saldo por cobrar que tenia el empleado justo antes de liquidar. Deja la
    -- foto del momento para poder auditar sin recalcular historia.
    BalanceBefore DECIMAL(14,2) NOT NULL,
    Currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    SettledAt DATETIME(6) NOT NULL,
    Note VARCHAR(255) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    -- Mismas razones que employee_balance: EmployeeId es referencia cross-servicio,
    -- no FK dura. Se indexa para el historial por empleado y por negocio.
    KEY idx_es_employee (EmployeeId),
    KEY idx_es_business (BusinessId),
    KEY idx_es_settled (SettledAt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- Menu del dueno: Liquidaciones (OWNER = 11111111-0000-0000-0000-000000000004)
-- ---------------------------------------------------------------------
SET @now = NOW(6);

INSERT INTO menu (Id, Code, Name, Icon, Route, ParentId, DisplayOrder, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT '77771001-0000-0000-0000-000000000012', 'TENANT_SETTLEMENTS', 'Liquidaciones', 'banknote', '/tenant/liquidaciones', NULL, 8, TRUE, TRUE, NULL, @now, @now
WHERE NOT EXISTS (SELECT 1 FROM menu WHERE Id = '77771001-0000-0000-0000-000000000012');

INSERT INTO menu_role (Id, MenuId, RoleId, Enabled, Visible, AuditUser, AuditDate, CreatedDate)
SELECT UUID(), m.Id, '11111111-0000-0000-0000-000000000004', TRUE, TRUE, NULL, @now, @now
FROM menu m
WHERE m.Id = '77771001-0000-0000-0000-000000000012'
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr
    WHERE mr.MenuId = m.Id
      AND mr.RoleId = '11111111-0000-0000-0000-000000000004'
);
