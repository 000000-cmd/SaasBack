-- =====================================================================
-- V4__1.0.0.sql
-- Saldo por cobrar del empleado (read model materializado).
--
-- El saldo es lo primero que el empleado ve en el APK. En vez de recalcularlo
-- al vuelo en cada consulta (join de servicios prestados x compensacion - pagos),
-- se MATERIALIZA en esta tabla y se refresca por evento cuando cambia una de sus
-- entradas. Ademas se proyecta a Elasticsearch (indice employee_balances) para
-- lecturas rapidas desde el APK.
--
-- Hoy AmountAccrued/AmountPaid arrancan en 0: no existe aun el modulo de
-- servicios prestados/pagos que los alimente. La estructura y la tuberia quedan
-- listas para cuando ese modulo emita sus eventos.
-- =====================================================================

SET @now = NOW(6);

CREATE TABLE employee_balance (
    Id CHAR(36) NOT NULL,
    BusinessId CHAR(36) NOT NULL,
    BranchId CHAR(36) NULL,
    EmployeeId CHAR(36) NOT NULL,
    ThirdPartyId CHAR(36) NULL,
    UserId CHAR(36) NULL,
    -- Devengado por servicios prestados (futuro modulo de agenda/servicios).
    AmountAccrued DECIMAL(14,2) NOT NULL DEFAULT 0,
    -- Pagado al empleado (futuro modulo de pagos).
    AmountPaid DECIMAL(14,2) NOT NULL DEFAULT 0,
    -- Por cobrar = AmountAccrued - AmountPaid. Denormalizado para leer directo.
    Balance DECIMAL(14,2) NOT NULL DEFAULT 0,
    Currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    LastCalculatedAt DATETIME(6) NULL,
    Enabled BOOLEAN NOT NULL DEFAULT TRUE, Visible BOOLEAN NOT NULL DEFAULT TRUE,
    CreatedBy CHAR(36) NULL, AuditUser CHAR(36) NULL, AuditDate DATETIME(6) NOT NULL, CreatedDate DATETIME(6) NOT NULL,
    PRIMARY KEY (Id),
    -- EmployeeId es una REFERENCIA al agregado de business-service, NO una FK dura:
    -- el saldo se inicializa (S2S) durante el aprovisionamiento, antes de que la
    -- transaccion de business confirme la fila de employee. Una FK cross-servicio
    -- la rechazaria. Se indexa como clave normal.
    UNIQUE KEY uq_eb_employee (EmployeeId),
    KEY idx_eb_business (BusinessId),
    KEY idx_eb_user (UserId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
