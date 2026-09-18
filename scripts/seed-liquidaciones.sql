-- =====================================================================
-- DATOS DE PRUEBA COMPLETOS PARA LIQUIDACIONES  (SQL puro, ejecutar y listo)
--
--   docker compose exec -T mysql mysql -uroot -pTU_PASSWORD saas_db < scripts/seed-liquidaciones.sql
--
-- (TU_PASSWORD es el MYSQL_ROOT_PASSWORD de tu .env)
--
-- Crea TODO lo necesario para entrar y probar la liquidación por servicio:
--   1) Un negocio con su dirección web, una sede y su dueño.
--   2) Tres empleados con cuenta propia y su compensación.
--   3) CARGOS DE SERVICIO que cubren TODOS los casos que la pantalla debe
--      saber manejar — no una lista bonita:
--        · transferencia SIN comprobante   -> dispara la alerta
--        · transferencia CON comprobante
--        · tarjeta SIN comprobante         -> también alerta
--        · efectivo                        -> no exige comprobante
--        · con foto del trabajo terminado
--        · ya confirmado                   -> suma al total a liquidar
--        · descartado con motivo           -> sale en el historial
--        · PENDIENTE DEL MES PASADO        -> el arrastrado, con su etiqueta
--   4) Saldos coherentes con esos servicios y los eventos que los proyectan a
--      Elasticsearch (de ahí lee la pantalla; sin eventos no se ve nada).
--   5) Imprime las credenciales al final.
--
-- Es idempotente: borra lo que sembró antes (por sus ids fijos) y lo rehace.
-- No toca datos que no sean de esta semilla.
-- =====================================================================

-- ---------------------------------------------------------------------
-- IMPRESCINDIBLE, y la razón no es obvia.
--
-- Las columnas de este esquema son utf8mb4_unicode_ci, pero un cliente moderno
-- (DataGrip, IntelliJ, MySQL Workbench, mysql 8.x recién instalado) abre la
-- conexión con utf8mb4_0900_ai_ci. En cuanto se compara una VARIABLE de sesión
-- con una columna —cosa que este script hace en cada línea— MySQL revienta con:
--
--   ERROR 1267: Illegal mix of collations
--     (utf8mb4_unicode_ci,IMPLICIT) and (utf8mb4_0900_ai_ci,IMPLICIT)
--
-- Esta línea alinea la colación de la conexión con la de las columnas y hace
-- que el script funcione igual desde cualquier cliente. No la borres.
-- ---------------------------------------------------------------------
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @now  := NOW(6);
SET @hoy  := CURDATE();

-- Ids fijos: permiten reejecutar sin duplicar y borrar sin arrastrar nada ajeno.
SET @biz    := 'aa000000-0000-0000-0000-00000000d001';
SET @branch := 'aa000000-0000-0000-0000-00000000b001';
SET @ownerU := 'aa000000-0000-0000-0000-00000000a000';
SET @ownerP := 'aa000000-0000-0000-0000-00000000c000';

-- La contraseña se copia del usuario `admin`, que crea el DataInitializer al
-- arrancar. Así todas las cuentas sembradas entran con la MISMA contraseña
-- conocida (Admin123!) sin tener que generar un hash BCrypt desde SQL.
SET @hash := (SELECT PasswordHash FROM app_user WHERE Username = 'admin' LIMIT 1);

-- Catálogos por CÓDIGO, no por id: así el script no depende de qué UUID sembró
-- la migración.
SET @bizType   := (SELECT Id FROM business_type WHERE Code = 'BARBERSHOP' LIMIT 1);
SET @brType    := (SELECT Id FROM branch_type   WHERE Code = 'BRANCH'     LIMIT 1);
SET @muni      := (SELECT Id FROM municipality  ORDER BY Name LIMIT 1);
SET @ownerRole := (SELECT Id FROM role          WHERE Code = 'OWNER'      LIMIT 1);
SET @docType   := (SELECT Id FROM document_type ORDER BY DisplayOrder LIMIT 1);

-- ---------------------------------------------------------------------
-- LIMPIEZA de una ejecución anterior, en orden inverso a las dependencias.
-- ---------------------------------------------------------------------
-- El sello de la liquidación apunta a employee_settlement: hay que soltarlo
-- antes de borrar las liquidaciones, o la clave foránea lo impide.
UPDATE service_charge SET SettlementId = NULL WHERE BusinessId = @biz;
DELETE FROM service_charge       WHERE BusinessId = @biz;
DELETE FROM employee_settlement  WHERE BusinessId = @biz;
-- payroll_run va DESPUÉS de employee_settlement: los movimientos de nómina
-- apuntan a la corrida por clave foránea.
DELETE FROM payroll_run          WHERE BusinessId = @biz;
DELETE FROM employee_balance     WHERE BusinessId = @biz;
DELETE FROM employee_compensation WHERE EmployeeId IN (SELECT Id FROM employee WHERE BranchId = @branch);
DELETE FROM business_compensation WHERE BusinessId = @biz;
DELETE FROM third_party_contact  WHERE ThirdPartyId LIKE 'aa000000-0000-0000-0000-00000000c%';
DELETE FROM bank_account         WHERE ThirdPartyId LIKE 'aa000000-0000-0000-0000-00000000c%';
DELETE FROM employee             WHERE BranchId = @branch;
DELETE FROM business_owner       WHERE BusinessId = @biz;
DELETE FROM branch               WHERE BusinessId = @biz;
DELETE FROM business_domain      WHERE BusinessId = @biz;
DELETE FROM business             WHERE Id = @biz;
DELETE FROM user_role            WHERE UserId LIKE 'aa000000-0000-0000-0000-00000000a%';
DELETE FROM third_party          WHERE Id LIKE 'aa000000-0000-0000-0000-00000000c%';
-- Las sesiones abiertas dejan filas en refresh_token con clave foránea al
-- usuario. Sin esto, reejecutar el script DESPUÉS de haber iniciado sesión
-- falla con "Cannot delete or update a parent row" — que es justo lo que pasa
-- la segunda vez que alguien lo usa.
DELETE FROM refresh_token        WHERE UserId LIKE 'aa000000-0000-0000-0000-00000000a%';
DELETE FROM app_user             WHERE Id LIKE 'aa000000-0000-0000-0000-00000000a%';

-- ---------------------------------------------------------------------
-- 1) NEGOCIO, DIRECCIÓN WEB, SEDE Y DUEÑO
-- ---------------------------------------------------------------------
INSERT INTO business (Id, BusinessTypeId, Name, Enabled, Visible, AuditDate, CreatedDate)
VALUES (@biz, @bizType, 'Barbería Aura', TRUE, TRUE, @now, @now);

-- El slug es el subdominio con el que se abre la página pública del negocio.
INSERT INTO business_domain (Id, BusinessId, Slug, Enabled, Visible, AuditDate, CreatedDate)
VALUES ('aa000000-0000-0000-0000-00000000f001', @biz, 'aura', TRUE, TRUE, @now, @now);

INSERT INTO branch (Id, BusinessId, BranchTypeId, Name, MunicipalityId, Enabled, Visible, AuditDate, CreatedDate)
VALUES (@branch, @biz, @brType, 'Sede Centro', @muni, TRUE, TRUE, @now, @now);

INSERT INTO app_user (Id, Username, Email, PasswordHash, Enabled, Visible, AuditDate, CreatedDate)
VALUES (@ownerU, 'camila', 'camila@aura.test', @hash, TRUE, TRUE, @now, @now);

INSERT INTO user_role (Id, UserId, RoleId, Enabled, Visible, AuditDate, CreatedDate)
VALUES (UUID(), @ownerU, @ownerRole, TRUE, TRUE, @now, @now);

INSERT INTO third_party
    (Id, DocumentTypeId, DocumentNumber, UserId, FirstName, FirstLastName,
     BiometricEnabled, Enabled, Visible, AuditDate, CreatedDate)
VALUES (@ownerP, @docType, '10000001', @ownerU, 'Camila', 'Restrepo',
        FALSE, TRUE, TRUE, @now, @now);

INSERT INTO business_owner
    (Id, BusinessId, ThirdPartyId, OwnershipPercentage, StartDate, Enabled, Visible, AuditDate, CreatedDate)
VALUES ('aa000000-0000-0000-0000-00000000d0a1', @biz, @ownerP, 100.00, @hoy, TRUE, TRUE, @now, @now);

-- ---------------------------------------------------------------------
-- 2) TRES EMPLEADOS, cada uno con su cuenta y su compensación.
--    Porcentajes distintos a propósito: así se ve que el deducible de cada
--    servicio sale de la compensación del empleado y no de un número fijo.
-- ---------------------------------------------------------------------
INSERT INTO app_user (Id, Username, Email, PasswordHash, Enabled, Visible, AuditDate, CreatedDate) VALUES
 ('aa000000-0000-0000-0000-00000000a001', 'sofia',   'sofia@aura.test',   @hash, TRUE, TRUE, @now, @now),
 ('aa000000-0000-0000-0000-00000000a002', 'andres',  'andres@aura.test',  @hash, TRUE, TRUE, @now, @now),
 ('aa000000-0000-0000-0000-00000000a003', 'valeria', 'valeria@aura.test', @hash, TRUE, TRUE, @now, @now);

INSERT INTO third_party
    (Id, DocumentTypeId, DocumentNumber, UserId, FirstName, FirstLastName,
     BiometricEnabled, Enabled, Visible, AuditDate, CreatedDate) VALUES
 ('aa000000-0000-0000-0000-00000000c001', @docType, '20000001', 'aa000000-0000-0000-0000-00000000a001', 'Sofía',   'Cardona', FALSE, TRUE, TRUE, @now, @now),
 ('aa000000-0000-0000-0000-00000000c002', @docType, '20000002', 'aa000000-0000-0000-0000-00000000a002', 'Andrés',  'Gómez',   FALSE, TRUE, TRUE, @now, @now),
 ('aa000000-0000-0000-0000-00000000c003', @docType, '20000003', 'aa000000-0000-0000-0000-00000000a003', 'Valeria', 'Ruiz',    FALSE, TRUE, TRUE, @now, @now);

INSERT INTO employee (Id, ThirdPartyId, BranchId, Enabled, Visible, AuditDate, CreatedDate) VALUES
 ('aa000000-0000-0000-0000-00000000e001', 'aa000000-0000-0000-0000-00000000c001', @branch, TRUE, TRUE, @now, @now),
 ('aa000000-0000-0000-0000-00000000e002', 'aa000000-0000-0000-0000-00000000c002', @branch, TRUE, TRUE, @now, @now),
 ('aa000000-0000-0000-0000-00000000e003', 'aa000000-0000-0000-0000-00000000c003', @branch, TRUE, TRUE, @now, @now);

-- Compensación del NEGOCIO: es donde vive la frecuencia con la que se dispersa
-- nómina. Quincenal a propósito, que es el caso interesante — el sueldo base se
-- abona en dos mitades y hay que ver que no se abone dos veces por quincena.
INSERT INTO business_compensation
    (Id, BusinessId, CompensationType, CompensationValue, SalaryBase, PayrollFrequency,
     ValidFrom, Enabled, Visible, AuditDate, CreatedDate)
VALUES ('aa000000-0000-0000-0000-0000000bc001', @biz, 'SERVICE_PERCENT_ONLY', 30.00, NULL, 'BIWEEKLY',
        @now, TRUE, TRUE, @now, @now);

-- Los tipos son los que entiende el resolutor de compensación. Sofía y Andrés
-- viven del porcentaje; VALERIA lleva SUELDO BASE además del porcentaje, que es
-- el caso que ejercita la tarea de abono programado y la línea extra del
-- comprobante ("además de la comisión, se te consignó el sueldo base").
INSERT INTO employee_compensation
    (Id, EmployeeId, CompensationType, CompensationValue, SalaryBase, ValidFrom, Enabled, Visible, AuditDate, CreatedDate) VALUES
 (UUID(), 'aa000000-0000-0000-0000-00000000e001', 'SERVICE_PERCENT_ONLY',        30.00, NULL,       @now, TRUE, TRUE, @now, @now),
 (UUID(), 'aa000000-0000-0000-0000-00000000e002', 'SERVICE_PERCENT_ONLY',        35.00, NULL,       @now, TRUE, TRUE, @now, @now),
 (UUID(), 'aa000000-0000-0000-0000-00000000e003', 'SALARY_PLUS_SERVICE_PERCENT', 40.00, 1400000.00, @now, TRUE, TRUE, @now, @now);

-- Correos PRINCIPALES Y VERIFICADOS: sin esto, liquidar y dispersar nómina
-- funcionan igual pero nadie recibe su aviso ni su extracto en PDF.
INSERT INTO third_party_contact
    (Id, ThirdPartyId, ContactTypeId, Value, IsPrimary, IsVerified, VerifiedAt,
     Enabled, Visible, AuditDate, CreatedDate)
-- El id tiene que ser HEXADECIMAL válido de punta a punta: la columna es un
-- UUID y una letra fuera de [0-9a-f] revienta al leerlo desde Java.
SELECT CONCAT('aa000000-0000-0000-0000-0000000cc', LPAD(ROW_NUMBER() OVER (ORDER BY u.Id), 3, '0')),
       tp.Id, (SELECT Id FROM contact_type WHERE Code = 'EMAIL' LIMIT 1), u.Email,
       TRUE, TRUE, @now, TRUE, TRUE, @now, @now
FROM third_party tp
JOIN app_user u ON u.Id = tp.UserId
WHERE tp.Id LIKE 'aa000000-0000-0000-0000-00000000c%';

-- Cuentas bancarias: es lo que el dueño necesita A LA MANO el día de pago.
-- Sofía con cuenta de banco, Andrés con DOS (para ver el selector y la
-- principal), y VALERIA con llave BREV — que es el caso que se guarda literal.
INSERT INTO bank_account
    (Id, ThirdPartyId, AccountKind, BankId, AccountType, AccountNumber, BrevKey, Alias, IsPrimary,
     Enabled, Visible, AuditDate, CreatedDate) VALUES
 ('aa000000-0000-0000-0000-0000000ba001', 'aa000000-0000-0000-0000-00000000c001', 'BANK',
  (SELECT Id FROM bank WHERE Code = 'BANCOLOMBIA'), 'SAVINGS', '91234567890', NULL, 'La de la nómina', TRUE,
  TRUE, TRUE, @now, @now),
 ('aa000000-0000-0000-0000-0000000ba002', 'aa000000-0000-0000-0000-00000000c002', 'BANK',
  (SELECT Id FROM bank WHERE Code = 'DAVIVIENDA'), 'SAVINGS', '00998877665', NULL, 'Principal', TRUE,
  TRUE, TRUE, @now, @now),
 ('aa000000-0000-0000-0000-0000000ba003', 'aa000000-0000-0000-0000-00000000c002', 'BANK',
  (SELECT Id FROM bank WHERE Code = 'NEQUI'), 'SAVINGS', '3005551122', NULL, 'Nequi', FALSE,
  TRUE, TRUE, @now, @now),
 ('aa000000-0000-0000-0000-0000000ba004', 'aa000000-0000-0000-0000-00000000c003', 'BREV',
  NULL, NULL, NULL, 'valeria@aura.test', 'Mi llave', TRUE,
  TRUE, TRUE, @now, @now);

-- ---------------------------------------------------------------------
-- 3) CARGOS DE SERVICIO
--    Los importes van CONGELADOS (bruto, deducible, neto): no se calculan al
--    leer, porque la compensación tiene vigencias y recalcular un servicio
--    viejo con la tarifa de hoy daría un número distinto al acordado.
-- ---------------------------------------------------------------------
INSERT INTO service_charge
    (Id, BusinessId, BranchId, EmployeeId, AppointmentId, ServiceName, ServiceDate, StartTime, EndTime,
     ClientName, ClientEmail, ClientPhone, GrossAmount, DeductionRate, DeductionAmount, NetAmount, Currency,
     PaymentMethod, ReceiptUrl, ResultPhotoUrl, Status, ConfirmedAt, DiscardedAt, DiscardReason,
     Enabled, Visible, AuditDate, CreatedDate) VALUES

 -- SOFÍA (30%) — transferencia SIN comprobante: la alerta que obliga a revisar el banco.
 ('aa000000-0000-0000-0000-00000000ca01', @biz, @branch, 'aa000000-0000-0000-0000-00000000e001', NULL,
  'Corte y secado', DATE_SUB(@hoy, INTERVAL 1 DAY), '09:00:00', '09:40:00',
  'Julián Pérez', 'julian.perez@correo.test', '+57 310 555 0101', 45000, 30.00, 13500, 31500, 'COP',
  'TRANSFER', NULL, NULL, 'PENDING', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 -- Transferencia CON comprobante y con foto del trabajo terminado.
 ('aa000000-0000-0000-0000-00000000ca02', @biz, @branch, 'aa000000-0000-0000-0000-00000000e001', NULL,
  'Tinte de raíz', DATE_SUB(@hoy, INTERVAL 2 DAY), '11:00:00', '12:15:00',
  'Laura Díaz', 'laura.diaz@correo.test', NULL, 80000, 30.00, 24000, 56000, 'COP',
  'TRANSFER', '/public/attachments/receipt/demo-1.jpg', '/public/attachments/result/demo-1.jpg',
  'PENDING', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 -- Efectivo: no exige comprobante, no debe alertar.
 ('aa000000-0000-0000-0000-00000000ca03', @biz, @branch, 'aa000000-0000-0000-0000-00000000e001', NULL,
  'Corte de cabello', DATE_SUB(@hoy, INTERVAL 4 DAY), '16:00:00', '16:30:00',
  'Marcela Pineda', NULL, '+57 311 555 0102', 35000, 30.00, 10500, 24500, 'COP',
  'CASH', NULL, NULL, 'PENDING', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 -- Ya confirmado: es lo que suma al total "listo para liquidar".
 ('aa000000-0000-0000-0000-00000000ca04', @biz, @branch, 'aa000000-0000-0000-0000-00000000e001', NULL,
  'Peinado para evento', DATE_SUB(@hoy, INTERVAL 6 DAY), '14:00:00', '15:30:00',
  'Alejandra Ramírez', 'ale.ramirez@correo.test', '+57 312 555 0103', 120000, 30.00, 36000, 84000, 'COP',
  'CASH', NULL, '/public/attachments/result/demo-2.jpg',
  'CONFIRMED', @now, NULL, NULL, TRUE, TRUE, @now, @now),

 -- Descartado con motivo: sale en el historial, no desaparece.
 ('aa000000-0000-0000-0000-00000000ca05', @biz, @branch, 'aa000000-0000-0000-0000-00000000e001', NULL,
  'Retoque de color', DATE_SUB(@hoy, INTERVAL 8 DAY), '10:00:00', '10:20:00',
  -- SIN contacto a proposito: el cliente de paso que no deja nada. No se le
  -- manda factura, y eso no es un fallo — es el caso normal de un salon.
  'Paula Ceballos', NULL, NULL, 20000, 30.00, 6000, 14000, 'COP',
  'CASH', NULL, NULL, 'DISCARDED', NULL, @now, 'El cliente no se presentó',
  TRUE, TRUE, @now, @now),

 -- EL ARRASTRADO: pendiente y de hace 40 días. Debe seguir apareciendo con su
 -- etiqueta de otro mes; si se ocultara, nadie cobraría por él.
 ('aa000000-0000-0000-0000-00000000ca06', @biz, @branch, 'aa000000-0000-0000-0000-00000000e001', NULL,
  'Alisado con keratina', DATE_SUB(@hoy, INTERVAL 40 DAY), '10:00:00', '13:00:00',
  'Diana Torres', 'diana.torres@correo.test', NULL, 200000, 30.00, 60000, 140000, 'COP',
  'TRANSFER', NULL, NULL, 'PENDING', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 -- ANDRÉS (35%) — tarjeta sin comprobante: el otro pago electrónico que alerta.
 ('aa000000-0000-0000-0000-00000000ca07', @biz, @branch, 'aa000000-0000-0000-0000-00000000e002', NULL,
  'Paquete barbería premium', DATE_SUB(@hoy, INTERVAL 2 DAY), '14:00:00', '16:30:00',
  'Carolina Vélez', 'caro.velez@correo.test', '+57 313 555 0104', 150000, 35.00, 52500, 97500, 'COP',
  'CARD', NULL, NULL, 'PENDING', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 ('aa000000-0000-0000-0000-00000000ca08', @biz, @branch, 'aa000000-0000-0000-0000-00000000e002', NULL,
  'Corte infantil', DATE_SUB(@hoy, INTERVAL 1 DAY), '10:00:00', '10:30:00',
  'Mateo Rincón', NULL, '+57 314 555 0105', 25000, 35.00, 8750, 16250, 'COP',
  'CASH', NULL, NULL, 'CONFIRMED', @now, NULL, NULL, TRUE, TRUE, @now, @now),

 -- VALERIA (40%) — todo pendiente: sirve para ver en el asistente a alguien
 -- que todavía no tiene nada aprobado y no se puede incluir.
 ('aa000000-0000-0000-0000-00000000ca09', @biz, @branch, 'aa000000-0000-0000-0000-00000000e003', NULL,
  'Depilación con cera', DATE_SUB(@hoy, INTERVAL 3 DAY), '15:00:00', '15:45:00',
  'Natalia Ossa', 'natalia.ossa@correo.test', NULL, 60000, 40.00, 24000, 36000, 'COP',
  'TRANSFER', NULL, NULL, 'PENDING', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 ('aa000000-0000-0000-0000-00000000ca10', @biz, @branch, 'aa000000-0000-0000-0000-00000000e003', NULL,
  'Pedicura spa', DATE_SUB(@hoy, INTERVAL 5 DAY), '09:30:00', '10:30:00',
  'Isabel Mejía', 'isabel.mejia@correo.test', '+57 315 555 0106', 55000, 40.00, 22000, 33000, 'COP',
  'CASH', NULL, NULL, 'PENDING', NULL, NULL, NULL, TRUE, TRUE, @now, @now);

-- ---------------------------------------------------------------------
-- 3b) HISTORIA: cuatro meses de servicios, generados.
--
--     Los de arriba cubren los CASOS RAROS (sin comprobante, arrastrado,
--     descartado) y por eso van escritos a mano. Estos son el volumen: sin
--     ellos, el comprobante muestra dos líneas y no se puede ver si el
--     desglose por servicio aguanta una factura de verdad.
--
--     Se generan con una CTE recursiva de días y aritmética modular: cada día
--     par produce un servicio, rotando empleado y tipo de servicio. Es
--     determinista, así que dos ejecuciones dan lo mismo.
-- ---------------------------------------------------------------------
INSERT INTO service_charge
    (Id, BusinessId, BranchId, EmployeeId, AppointmentId, ServiceName, ServiceDate, StartTime, EndTime,
     ClientName, ClientEmail, ClientPhone, GrossAmount, DeductionRate, DeductionAmount, NetAmount, Currency,
     PaymentMethod, ReceiptUrl, ResultPhotoUrl, Status, ConfirmedAt, DiscardedAt, DiscardReason,
     Enabled, Visible, AuditDate, CreatedDate)
WITH RECURSIVE dias (n) AS (
    SELECT 8 UNION ALL SELECT n + 1 FROM dias WHERE n < 125
),
-- CADA EMPLEADO TIENE SU OFICIO. El catálogo va indexado por empleado (emp) y
-- no suelto: repartir los ocho servicios entre los tres al azar hacía que al
-- barbero le salieran pedicuras y balayages en su comprobante. La atribución
-- era correcta —el EmployeeId siempre fue el suyo— pero se LEÍA como un error,
-- y un dato de prueba que parece un fallo es tan malo como el fallo: obliga a
-- verificar la base cada vez que alguien mira un recibo.
--
-- Con esto, una pedicura en el recibo de Andrés SÍ es un fallo, y se ve.
servicios (emp, i, nombre, precio) AS (
    -- Sofía (e001): peluquería y color
              SELECT 0, 0, 'Corte de cabello',           35000
    UNION ALL SELECT 0, 1, 'Tinte de cabello',          120000
    UNION ALL SELECT 0, 2, 'Balayage completo',         180000
    UNION ALL SELECT 0, 3, 'Peinado para evento',       110000
    -- Andrés (e002): barbería
    UNION ALL SELECT 1, 0, 'Corte de cabello',           35000
    UNION ALL SELECT 1, 1, 'Corte + barba',              45000
    UNION ALL SELECT 1, 2, 'Corte infantil',             25000
    UNION ALL SELECT 1, 3, 'Perfilado de barba',         30000
    -- Valeria (e003): uñas y estética
    UNION ALL SELECT 2, 0, 'Manicura semipermanente',    80000
    UNION ALL SELECT 2, 1, 'Pedicura spa',               55000
    UNION ALL SELECT 2, 2, 'Depilación con cera',        60000
    UNION ALL SELECT 2, 3, 'Diseño de cejas',            40000
),
-- NINGUNO puede llamarse como un empleado (Sofía Cardona, Andrés Gómez,
-- Valeria Ruiz) ni como la dueña (Camila Restrepo). Reusar esos nombres como
-- clientes hacía que una factura correcta PARECIERA llevar el trabajo de otra
-- persona, que es peor que un fallo real: se ve como uno.
-- El contacto va MEZCLADO a propósito: unos con correo, otros solo con
-- teléfono, otros sin nada. Es lo que ejercita las tres ramas del envío de la
-- factura del cliente (adjunto, enlace, y no mandar nada).
clientes (i, nombre, correo, telefono) AS (
             SELECT 0, 'María Fernanda Quintero', 'mafe.quintero@correo.test', '+57 300 555 0201'
    UNION ALL SELECT 1, 'Tomás Beltrán',          'tomas.beltran@correo.test', NULL
    UNION ALL SELECT 2, 'Lucía Arango',            NULL,                       '+57 301 555 0202'
    UNION ALL SELECT 3, 'Emilio Castaño',         'emilio.castano@correo.test', '+57 302 555 0203'
    UNION ALL SELECT 4, 'Renata Villamil',         NULL,                        NULL
    UNION ALL SELECT 5, 'Gabriel Ocampo',         'gabriel.ocampo@correo.test', NULL
    UNION ALL SELECT 6, 'Ximena Bustos',          'ximena.bustos@correo.test', '+57 304 555 0204'
),
empleados (i, id, tasa) AS (
             SELECT 0, 'aa000000-0000-0000-0000-00000000e001', 30.00
    UNION ALL SELECT 1, 'aa000000-0000-0000-0000-00000000e002', 35.00
    UNION ALL SELECT 2, 'aa000000-0000-0000-0000-00000000e003', 40.00
)
SELECT
    UUID(), @biz, @branch, e.id, NULL,
    s.nombre,
    DATE_SUB(@hoy, INTERVAL d.n DAY),
    SEC_TO_TIME(((d.n % 8) + 8) * 3600),
    SEC_TO_TIME((((d.n % 8) + 9) * 3600) + 1800),
    c.nombre,
    c.correo,
    c.telefono,
    s.precio,
    e.tasa,
    ROUND(s.precio * e.tasa / 100),
    s.precio - ROUND(s.precio * e.tasa / 100),
    'COP',
    -- Rota los tres medios de pago; los electrónicos llevan comprobante salvo
    -- uno de cada seis, que es el caso que enciende la alerta en la pantalla.
    CASE d.n % 3 WHEN 0 THEN 'CASH' WHEN 1 THEN 'TRANSFER' ELSE 'CARD' END,
    CASE WHEN d.n % 3 <> 0 AND d.n % 6 <> 1
         THEN '/public/attachments/receipt/demo-1.jpg' END,
    CASE WHEN d.n % 5 = 0 THEN '/public/attachments/result/demo-1.jpg' END,
    -- Lo de meses anteriores ya está resuelto; lo del mes en curso sigue vivo
    -- para poder liquidarlo a mano en la prueba.
    CASE WHEN DATE_SUB(@hoy, INTERVAL d.n DAY) < DATE_FORMAT(@hoy, '%Y-%m-01')
              THEN 'CONFIRMED' ELSE 'PENDING' END,
    CASE WHEN DATE_SUB(@hoy, INTERVAL d.n DAY) < DATE_FORMAT(@hoy, '%Y-%m-01')
              THEN @now END,
    NULL, NULL,
    TRUE, TRUE, @now, @now
FROM dias d
-- Se rota sobre n DIV 2 y no sobre n: como solo entran los días pares, con n
-- directo los ciclos de 8 y de 6 se quedaban en los índices pares y siempre
-- salían los mismos cuatro servicios y el mismo cliente.
JOIN empleados e ON e.i = (d.n DIV 2) % 3
-- El servicio se elige DENTRO del repertorio del empleado que salió. Los ciclos
-- 3 y 4 son primos entre sí, así que cada uno acaba prestando sus cuatro.
JOIN servicios s ON s.emp = e.i AND s.i = (d.n DIV 2) % 4
JOIN clientes  c ON c.i = (d.n DIV 2) % 7
WHERE d.n % 2 = 0;

-- ---------------------------------------------------------------------
-- 3c) LA AGENDA: servicios AGENDADOS de hoy y de los próximos días.
--
--     Es lo que el empleado ve al abrir su app y lo único sobre lo que actúa:
--     marca terminado el de hoy y pasa a esperar aprobación. Sin estas filas la
--     app arranca vacía y no se puede probar el ciclo completo.
--
--     El módulo de citas todavía no existe, así que la "cita" es este mismo
--     cargo con fecha futura: ya trae hora, cliente, servicio y precio, que es
--     exactamente lo que una cita necesita. Cuando llegue el módulo, estas filas
--     se enlazan por AppointmentId sin migrar nada.
-- ---------------------------------------------------------------------
INSERT INTO service_charge
    (Id, BusinessId, BranchId, EmployeeId, AppointmentId, ServiceName, ServiceDate, StartTime, EndTime,
     ClientName, ClientEmail, ClientPhone, GrossAmount, DeductionRate, DeductionAmount, NetAmount, Currency,
     PaymentMethod, ReceiptUrl, ResultPhotoUrl, Status, ConfirmedAt, DiscardedAt, DiscardReason,
     Enabled, Visible, AuditDate, CreatedDate) VALUES

 -- HOY, de Andrés: los tres que puede dar por terminados desde su app.
 ('aa000000-0000-0000-0000-00000000cb01', @biz, @branch, 'aa000000-0000-0000-0000-00000000e002', NULL,
  'Corte + barba', @hoy, '09:00:00', '09:45:00',
  'Tomás Beltrán', 'tomas.beltran@correo.test', '+57 300 555 0301', 45000, 35.00, 15750, 29250, 'COP',
  'CASH', NULL, NULL, 'SCHEDULED', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 ('aa000000-0000-0000-0000-00000000cb02', @biz, @branch, 'aa000000-0000-0000-0000-00000000e002', NULL,
  'Corte infantil', @hoy, '11:30:00', '12:00:00',
  'Emilio Castaño', NULL, '+57 301 555 0302', 25000, 35.00, 8750, 16250, 'COP',
  'TRANSFER', NULL, NULL, 'SCHEDULED', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 ('aa000000-0000-0000-0000-00000000cb03', @biz, @branch, 'aa000000-0000-0000-0000-00000000e002', NULL,
  'Perfilado de barba', @hoy, '16:00:00', '16:30:00',
  'Gabriel Ocampo', 'gabriel.ocampo@correo.test', NULL, 30000, 35.00, 10500, 19500, 'COP',
  'CARD', NULL, NULL, 'SCHEDULED', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 -- HOY, de Sofía y Valeria: para que las tres cuentas tengan algo que hacer.
 ('aa000000-0000-0000-0000-00000000cb04', @biz, @branch, 'aa000000-0000-0000-0000-00000000e001', NULL,
  'Balayage completo', @hoy, '10:00:00', '13:00:00',
  'Renata Villamil', NULL, NULL, 180000, 30.00, 54000, 126000, 'COP',
  'TRANSFER', NULL, NULL, 'SCHEDULED', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 ('aa000000-0000-0000-0000-00000000cb05', @biz, @branch, 'aa000000-0000-0000-0000-00000000e003', NULL,
  'Manicura semipermanente', @hoy, '14:00:00', '15:15:00',
  'Ximena Bustos', 'ximena.bustos@correo.test', '+57 304 555 0204', 80000, 40.00, 32000, 48000, 'COP',
  'CASH', NULL, NULL, 'SCHEDULED', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 -- PRÓXIMOS DÍAS: la agenda que se ve pero sobre la que todavía no se actúa.
 ('aa000000-0000-0000-0000-00000000cb06', @biz, @branch, 'aa000000-0000-0000-0000-00000000e002', NULL,
  'Corte de cabello', DATE_ADD(@hoy, INTERVAL 1 DAY), '08:30:00', '09:00:00',
  'Lucía Arango', NULL, '+57 301 555 0202', 35000, 35.00, 12250, 22750, 'COP',
  'CASH', NULL, NULL, 'SCHEDULED', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 ('aa000000-0000-0000-0000-00000000cb07', @biz, @branch, 'aa000000-0000-0000-0000-00000000e002', NULL,
  'Paquete barbería premium', DATE_ADD(@hoy, INTERVAL 2 DAY), '15:00:00', '17:00:00',
  'María Fernanda Quintero', 'mafe.quintero@correo.test', '+57 300 555 0201', 150000, 35.00, 52500, 97500, 'COP',
  'CARD', NULL, NULL, 'SCHEDULED', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 ('aa000000-0000-0000-0000-00000000cb08', @biz, @branch, 'aa000000-0000-0000-0000-00000000e001', NULL,
  'Peinado para evento', DATE_ADD(@hoy, INTERVAL 3 DAY), '17:00:00', '18:30:00',
  'Alejandra Ramírez', 'ale.ramirez@correo.test', '+57 312 555 0103', 110000, 30.00, 33000, 77000, 'COP',
  'TRANSFER', NULL, NULL, 'SCHEDULED', NULL, NULL, NULL, TRUE, TRUE, @now, @now),

 ('aa000000-0000-0000-0000-00000000cb09', @biz, @branch, 'aa000000-0000-0000-0000-00000000e003', NULL,
  'Pedicura spa', DATE_ADD(@hoy, INTERVAL 4 DAY), '09:00:00', '10:00:00',
  'Isabel Mejía', 'isabel.mejia@correo.test', '+57 315 555 0106', 55000, 40.00, 22000, 33000, 'COP',
  'CASH', NULL, NULL, 'SCHEDULED', NULL, NULL, NULL, TRUE, TRUE, @now, @now);

-- ---------------------------------------------------------------------
-- 4) SALDOS EN CERO.
--
--    El saldo NO nace de tener servicios: nace de LIQUIDARLOS. Un servicio
--    pendiente o aprobado pero sin liquidar todavía no está en el saldo de
--    nadie, y sembrarlo ahí haría que Nómina ofreciera pagar dinero que nunca
--    se aprobó. Es exactamente la confusión que este refactor viene a quitar.
-- ---------------------------------------------------------------------
INSERT INTO employee_balance
    (Id, BusinessId, BranchId, EmployeeId, ThirdPartyId, UserId,
     AmountAccrued, AmountPaid, Balance, Currency, LastCalculatedAt,
     Enabled, Visible, AuditDate, CreatedDate)
SELECT
    -- El id tiene que ser un UUID VÁLIDO: el evento del outbox lo pasa por
    -- UUID_TO_BIN, que exige 36 caracteres con el último grupo de 12 hex.
    CONCAT('aa000000-0000-0000-0000-0000000eb', LPAD(ROW_NUMBER() OVER (ORDER BY e.Id), 3, '0')),
    @biz, @branch, e.Id, e.ThirdPartyId, tp.UserId,
    0, 0, 0, 'COP', @now,
    TRUE, TRUE, @now, @now
FROM employee e
JOIN third_party tp ON tp.Id = e.ThirdPartyId
WHERE e.BranchId = @branch;

-- ---------------------------------------------------------------------
-- 4b) DOS TANDAS DE LIQUIDACIÓN, una ya pagada y otra por pagar.
--
--     Se siembra EXACTAMENTE como lo haría la app (movimiento COMMISSION, los
--     cargos sellados con su id y el saldo subido) para que el estado inicial
--     sea uno al que la aplicación puede llegar sola.
--
--     TANDA 1 — lo de hace más de un mes. Liquidada Y dispersada: es la que
--     deja un comprobante con muchos servicios debajo, que es donde se ve si
--     el desglose aguanta una factura de verdad.
--     TANDA 2 — lo del mes pasado. Liquidada pero SIN pagar: deja saldo a favor
--     para poder dispersar de verdad al abrir la pantalla.
--     Lo del mes en curso queda PENDIENTE, para probar el ciclo entero.
-- ---------------------------------------------------------------------
SET @corte1 := DATE_SUB(DATE_FORMAT(@hoy, '%Y-%m-01'), INTERVAL 1 MONTH); -- inicio del mes pasado
SET @fecha1 := DATE_SUB(@corte1, INTERVAL 1 DAY);                         -- se liquidó al cerrar
SET @fecha2 := DATE_SUB(DATE_FORMAT(@hoy, '%Y-%m-01'), INTERVAL 1 DAY);   -- cierre del mes pasado

-- Sufijo estable por empleado: hace falta para que cada liquidación, cada pago
-- y cada sello apunten al mismo sitio sin depender del orden de las filas.
DROP TEMPORARY TABLE IF EXISTS emp_ix;
CREATE TEMPORARY TABLE emp_ix (Id CHAR(36) PRIMARY KEY, ix INT);
INSERT INTO emp_ix (Id, ix) VALUES
 ('aa000000-0000-0000-0000-00000000e001', 1),
 ('aa000000-0000-0000-0000-00000000e002', 2),
 ('aa000000-0000-0000-0000-00000000e003', 3);

-- ----- TANDA 1: liquidación de lo más viejo -----
INSERT INTO employee_settlement
    (Id, BusinessId, BranchId, EmployeeId, Amount, BalanceBefore, Currency, SettledAt, Note,
     MovementType, CommissionAmount, BaseSalaryAmount, PaidInCash,
     Enabled, Visible, AuditDate, CreatedDate)
SELECT CONCAT('aa000000-0000-0000-0000-0000000fa00', ix.ix),
       @biz, @branch, sc.EmployeeId, SUM(sc.NetAmount), 0, 'COP', @fecha1,
       'Liquidación de cierre', 'COMMISSION', SUM(sc.NetAmount), 0, FALSE,
       TRUE, TRUE, @now, @now
FROM service_charge sc
JOIN emp_ix ix ON ix.Id = sc.EmployeeId
WHERE sc.BusinessId = @biz AND sc.Status = 'CONFIRMED' AND sc.SettlementId IS NULL
  AND sc.ServiceDate < @corte1
GROUP BY sc.EmployeeId, ix.ix;

UPDATE service_charge sc
  JOIN emp_ix ix ON ix.Id = sc.EmployeeId
   SET sc.SettlementId = CONCAT('aa000000-0000-0000-0000-0000000fa00', ix.ix)
 WHERE sc.BusinessId = @biz AND sc.Status = 'CONFIRMED' AND sc.SettlementId IS NULL
   AND sc.ServiceDate < @corte1;

-- ----- La corrida que pagó la tanda 1 -----
INSERT INTO payroll_run
    (Id, BusinessId, BranchId, Code, PeriodLabel, PeriodStart, PeriodEnd,
     EmployeeCount, TotalAmount, Currency, Status, ExecutedAt,
     Enabled, Visible, AuditDate, CreatedDate)
SELECT 'aa000000-0000-0000-0000-0000000fe001', @biz, @branch,
       CONCAT('DP-', DATE_FORMAT(@fecha1, '%Y%m%d'), '-A001'),
       CONCAT('Cierre de ', DATE_FORMAT(@fecha1, '%m/%Y')),
       DATE_FORMAT(@fecha1, '%Y-%m-01'), @fecha1,
       COUNT(*), SUM(es.Amount), 'COP', 'COMPLETED', DATE_ADD(@fecha1, INTERVAL 1 DAY),
       TRUE, TRUE, @now, @now
FROM employee_settlement es
WHERE es.Id LIKE 'aa000000-0000-0000-0000-0000000fa00%';

-- Un pago por empleado. El segundo va EN EFECTIVO y sin acusar: es lo que
-- enciende el aviso de confirmación en la app del colaborador.
INSERT INTO employee_settlement
    (Id, BusinessId, BranchId, EmployeeId, Amount, BalanceBefore, Currency, SettledAt, Note,
     MovementType, PayrollRunId, CommissionAmount, BaseSalaryAmount,
     PayoutAccount, BankAccountId, PaymentProofUrl, PaidInCash, CashConfirmedAt,
     Enabled, Visible, AuditDate, CreatedDate)
SELECT CONCAT('aa000000-0000-0000-0000-0000000fc00', ix.ix),
       @biz, @branch, es.EmployeeId, es.Amount, es.Amount, 'COP',
       DATE_ADD(@fecha1, INTERVAL 1 DAY), 'Pago de nómina',
       'PAYROLL', 'aa000000-0000-0000-0000-0000000fe001', es.Amount, 0,
       CASE WHEN ix.ix = 2 THEN 'Efectivo' ELSE ba.Alias END,
       CASE WHEN ix.ix = 2 THEN NULL ELSE ba.Id END,
       CASE WHEN ix.ix = 2 THEN NULL ELSE '/public/attachments/payroll/demo-1.jpg' END,
       ix.ix = 2,
       NULL,
       TRUE, TRUE, @now, @now
FROM employee_settlement es
JOIN emp_ix ix ON ix.Id = es.EmployeeId
LEFT JOIN bank_account ba ON ba.ThirdPartyId = (
        SELECT e.ThirdPartyId FROM employee e WHERE e.Id = es.EmployeeId
    ) AND ba.IsPrimary = TRUE
WHERE es.Id LIKE 'aa000000-0000-0000-0000-0000000fa00%';

-- ----- TANDA 2: liquidación del mes pasado, TODAVÍA SIN PAGAR -----
INSERT INTO employee_settlement
    (Id, BusinessId, BranchId, EmployeeId, Amount, BalanceBefore, Currency, SettledAt, Note,
     MovementType, CommissionAmount, BaseSalaryAmount, PaidInCash,
     Enabled, Visible, AuditDate, CreatedDate)
SELECT CONCAT('aa000000-0000-0000-0000-0000000fb00', ix.ix),
       @biz, @branch, sc.EmployeeId, SUM(sc.NetAmount), 0, 'COP', @fecha2,
       'Liquidación del mes', 'COMMISSION', SUM(sc.NetAmount), 0, FALSE,
       TRUE, TRUE, @now, @now
FROM service_charge sc
JOIN emp_ix ix ON ix.Id = sc.EmployeeId
WHERE sc.BusinessId = @biz AND sc.Status = 'CONFIRMED' AND sc.SettlementId IS NULL
GROUP BY sc.EmployeeId, ix.ix;

UPDATE service_charge sc
  JOIN emp_ix ix ON ix.Id = sc.EmployeeId
   SET sc.SettlementId = CONCAT('aa000000-0000-0000-0000-0000000fb00', ix.ix)
 WHERE sc.BusinessId = @biz AND sc.Status = 'CONFIRMED' AND sc.SettlementId IS NULL;

-- ----- El saldo, recalculado desde los movimientos (como hace el back) -----
UPDATE employee_balance eb
   SET eb.AmountAccrued = IFNULL((SELECT SUM(m.Amount) FROM employee_settlement m
                                   WHERE m.EmployeeId = eb.EmployeeId
                                     AND m.MovementType IN ('COMMISSION','BASE_SALARY')), 0),
       eb.AmountPaid    = IFNULL((SELECT SUM(m.Amount) FROM employee_settlement m
                                   WHERE m.EmployeeId = eb.EmployeeId
                                     AND m.MovementType = 'PAYROLL'), 0)
 WHERE eb.BusinessId = @biz;

UPDATE employee_balance eb
   SET eb.Balance = eb.AmountAccrued - eb.AmountPaid
 WHERE eb.BusinessId = @biz;

DROP TEMPORARY TABLE IF EXISTS emp_ix;

-- ---------------------------------------------------------------------
-- 5) Un evento por saldo para que se proyecte a Elasticsearch. La pantalla lee
--    de ahí: sin esto no se ve nada aunque las filas existan.
-- ---------------------------------------------------------------------
INSERT INTO outbox_event
    (Id, EventId, AggregateType, AggregateId, EventType, Version, BusinessId, Payload, Status, Retries, CreatedAt)
SELECT
    UUID_TO_BIN(UUID(), 0), UUID_TO_BIN(UUID(), 0),
    'employee_balance', UUID_TO_BIN(eb.Id, 0),
    'finance.balance.updated', 1, UUID_TO_BIN(eb.BusinessId, 0),
    JSON_OBJECT(
        'id',               eb.Id,
        'businessId',       eb.BusinessId,
        'branchId',         eb.BranchId,
        'employeeId',       eb.EmployeeId,
        'thirdPartyId',     eb.ThirdPartyId,
        'userId',           eb.UserId,
        'amountAccrued',    CAST(eb.AmountAccrued AS DOUBLE),
        'amountPaid',       CAST(IFNULL(eb.AmountPaid, 0) AS DOUBLE),
        'balance',          CAST(eb.Balance AS DOUBLE),
        'currency',         IFNULL(eb.Currency, 'COP'),
        -- Fecha ISO local SIN zona, como la publica finance. Con zona (o sin
        -- milisegundos) el documento de Elastic falla al leerse.
        'lastCalculatedAt', DATE_FORMAT(@now, '%Y-%m-%dT%H:%i:%s.%f')
    ),
    'PENDING', 0, @now
FROM employee_balance eb
WHERE eb.BusinessId = @biz;

-- ---------------------------------------------------------------------
-- 6) RESUMEN Y CREDENCIALES
-- ---------------------------------------------------------------------
SELECT '=========== LISTO. ENTRA CON ESTO ===========' AS '';

SELECT 'DUEÑO — panel del negocio (:4200)' AS quien, 'camila'  AS usuario, 'Admin123!' AS contrasena
UNION ALL SELECT 'Empleado — app móvil',              'sofia',   'Admin123!'
UNION ALL SELECT 'Empleado — app móvil',              'andres',  'Admin123!'
UNION ALL SELECT 'Empleado — app móvil',              'valeria', 'Admin123!'
UNION ALL SELECT 'Admin del sistema (:4201)',         'admin',   'Admin123!';

SELECT '--- Servicios sembrados por empleado ---' AS '';

SELECT
    CONCAT(tp.FirstName, ' ', tp.FirstLastName) AS empleado,
    -- Agendados de hoy: es sobre lo único que el empleado actúa al abrir su app.
    SUM(sc.Status = 'SCHEDULED' AND sc.ServiceDate = @hoy) AS hoy,
    SUM(sc.Status = 'SCHEDULED')                AS agendados,
    SUM(sc.Status = 'PENDING')                  AS pendientes,
    SUM(sc.Status = 'CONFIRMED')                AS confirmados,
    SUM(sc.Status = 'DISCARDED')                AS descartados,
    SUM(sc.Status = 'PENDING'
        AND sc.PaymentMethod IN ('TRANSFER','CARD')
        AND sc.ReceiptUrl IS NULL)              AS sin_comprobante,
    SUM(sc.Status = 'PENDING'
        AND sc.ServiceDate < DATE_FORMAT(@hoy, '%Y-%m-01')) AS de_otro_mes,
    -- Aprobado y TODAVÍA sin liquidar. Los que ya se liquidaron arriba pasaron
    -- al saldo: contarlos aquí haría creer que están dos veces.
    FORMAT(SUM(CASE WHEN sc.Status = 'CONFIRMED' AND sc.SettlementId IS NULL
                    THEN sc.NetAmount ELSE 0 END), 0) AS sin_liquidar
FROM service_charge sc
JOIN employee e     ON e.Id = sc.EmployeeId
JOIN third_party tp ON tp.Id = e.ThirdPartyId
WHERE sc.BusinessId = @biz
GROUP BY tp.FirstName, tp.FirstLastName
ORDER BY empleado;

-- El repertorio de cada uno, de un vistazo. Si aquí aparece una pedicura bajo
-- el barbero, el fallo está en la semilla y no en la liquidación: sirve para no
-- volver a confundir "dato raro" con "servicio mal atribuido".
SELECT '--- Qué presta cada empleado (cada uno solo lo suyo) ---' AS '';

SELECT CONCAT(tp.FirstName, ' ', tp.FirstLastName)             AS empleado,
       GROUP_CONCAT(DISTINCT sc.ServiceName ORDER BY sc.ServiceName SEPARATOR ' · ') AS servicios
FROM service_charge sc
JOIN employee e     ON e.Id = sc.EmployeeId
JOIN third_party tp ON tp.Id = e.ThirdPartyId
WHERE sc.BusinessId = @biz
GROUP BY empleado
ORDER BY empleado;

SELECT '--- Saldo a favor (lo que Nómina puede dispersar) ---' AS '';

SELECT
    CONCAT(tp.FirstName, ' ', tp.FirstLastName) AS empleado,
    FORMAT(eb.Balance, 0)                       AS saldo_a_favor,
    IFNULL(ec.CompensationType, '—')            AS compensacion,
    IFNULL(FORMAT(ec.SalaryBase, 0), '—')       AS sueldo_base_mensual
FROM employee_balance eb
JOIN employee e     ON e.Id = eb.EmployeeId
JOIN third_party tp ON tp.Id = e.ThirdPartyId
LEFT JOIN employee_compensation ec ON ec.EmployeeId = e.Id AND ec.ValidTo IS NULL
WHERE eb.BusinessId = @biz
ORDER BY empleado;

-- ---------------------------------------------------------------------
-- 7) CUADRE. Lo que se siembra tiene que ser defendible peso a peso: cada pago
--    debe sumar EXACTAMENTE los servicios que dice cubrir, ningún servicio
--    puede colgar de la liquidación de otra persona y ningún nombre de cliente
--    puede coincidir con un empleado. Si alguna línea sale distinta de 0 / de
--    CUADRA, la semilla está mal y no hay que confiar en las pruebas.
-- ---------------------------------------------------------------------
SELECT '--- Cuadre de la semilla (todo debe salir en 0 / CUADRA) ---' AS '';

SELECT
    (SELECT COUNT(*) FROM service_charge sc
      JOIN employee_settlement liq ON liq.Id = sc.SettlementId
     WHERE liq.EmployeeId <> sc.EmployeeId)                        AS servicios_de_otro_empleado,
    (SELECT COUNT(*) FROM service_charge sc
      JOIN third_party tp ON CONCAT(tp.FirstName,' ',tp.FirstLastName) = sc.ClientName)
                                                                    AS clientes_con_nombre_de_empleado;

SELECT
    CONCAT(tp.FirstName, ' ', tp.FirstLastName)                     AS empleado,
    FORMAT(p.CommissionAmount, 0)                                   AS comision_pagada,
    FORMAT(IFNULL(SUM(sc.NetAmount), 0), 0)                         AS suma_de_sus_servicios,
    COUNT(sc.Id)                                                    AS servicios,
    IF(p.CommissionAmount = IFNULL(SUM(sc.NetAmount), 0),
       'CUADRA', '*** DESCUADRE ***')                               AS veredicto
FROM employee_settlement p
JOIN employee e     ON e.Id = p.EmployeeId
JOIN third_party tp ON tp.Id = e.ThirdPartyId
-- La misma ventana que usa el back: las liquidaciones abonadas DESPUÉS del pago
-- anterior y hasta este. Se reproduce aquí a propósito, para que el cuadre sea
-- una comprobación independiente y no un eco de la misma consulta.
LEFT JOIN employee_settlement liq
       ON liq.EmployeeId = p.EmployeeId
      AND liq.MovementType = 'COMMISSION'
      AND liq.SettledAt <= p.SettledAt
      AND liq.SettledAt > IFNULL((SELECT MAX(ant.SettledAt) FROM employee_settlement ant
                                   WHERE ant.EmployeeId = p.EmployeeId
                                     AND ant.MovementType = 'PAYROLL'
                                     AND ant.SettledAt < p.SettledAt), '1970-01-01')
LEFT JOIN service_charge sc ON sc.SettlementId = liq.Id AND sc.EmployeeId = p.EmployeeId
WHERE p.MovementType = 'PAYROLL' AND p.BusinessId = @biz
GROUP BY p.Id, empleado, p.CommissionAmount
ORDER BY empleado;

SELECT CONCAT(
    'Negocio "Barbería Aura" (subdominio: aura). ',
    COUNT(*), ' eventos de saldo encolados: la proyección a Elasticsearch tarda ',
    'unos segundos. Recorrido: /tenant/liquidaciones para aprobar y abonar, ',
    'luego /tenant/nomina para consignar. Valeria lleva sueldo base: pulsa ',
    '"Abonar ahora" en Nómina para ver el abono del periodo.'
) AS ''
FROM outbox_event
WHERE Status = 'PENDING' AND EventType = 'finance.balance.updated';
