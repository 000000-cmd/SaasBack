-- ---------------------------------------------------------------------------
-- Hallazgos 6 y 7 del inventario: anular una corrida de nomina, y saber cuando
-- el mismo comprobante respalda varios pagos.
-- ---------------------------------------------------------------------------

USE saas_db;

-- ---------------------------------------------------------------------
-- HALLAZGO 6 — anular una corrida
--
-- Anular NO borra. El libro es de solo anadir: la fila de PAYROLL dice que la
-- plata salio ese dia, y eso paso. Lo que se hace es escribir el
-- CONTRA-MOVIMIENTO, que dice que se deshizo — y el saldo vuelve por la suma
-- de los dos, no por la desaparicion de uno.
--
-- Sin esto, un pago mal hecho no se podia corregir de ninguna forma: ni
-- borrando (el libro no deja) ni pagando de menos la vez siguiente (el saldo
-- no cuadraria nunca).
-- ---------------------------------------------------------------------
SET @hay := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'payroll_run'
               AND COLUMN_NAME = 'VoidedAt');
SET @ddl := IF(@hay = 0,
    'ALTER TABLE payroll_run
       ADD COLUMN VoidedAt DATETIME(6) NULL COMMENT ''Cuando se anulo. NULL = sigue en pie'' AFTER ExecutedAt,
       ADD COLUMN VoidReason VARCHAR(300) NULL COMMENT ''Por que se anulo. Obligatorio al anular'' AFTER VoidedAt',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- El contra-movimiento apunta al que deshace. Con esto, el extracto de un
-- empleado se lee entero: "se te pago X" y debajo "se anulo X".
SET @hay2 := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'employee_settlement'
                AND COLUMN_NAME = 'ReversalOfId');
SET @ddl2 := IF(@hay2 = 0,
    'ALTER TABLE employee_settlement
       ADD COLUMN ReversalOfId CHAR(36) NULL COMMENT ''El movimiento que este deshace'' AFTER PayrollRunId,
       ADD KEY idx_es_reversal (ReversalOfId)',
    'DO 0');
PREPARE stmt2 FROM @ddl2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- Un movimiento se deshace UNA vez. Sin esta clave, anular dos veces la misma
-- corrida devolveria el saldo dos veces — y la segunda es dinero inventado.
SET @hay3 := (SELECT COUNT(*) FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'employee_settlement'
                AND INDEX_NAME = 'uq_es_reversal');
SET @ddl3 := IF(@hay3 = 0,
    'ALTER TABLE employee_settlement ADD UNIQUE KEY uq_es_reversal (ReversalOfId)',
    'DO 0');
PREPARE stmt3 FROM @ddl3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;

-- ---------------------------------------------------------------------
-- HALLAZGO 7 — el mismo comprobante en dos pagos
--
-- Se guarda el SHA-256 del fichero, no un "es duplicado" calculado: un
-- indicador guardado se queda viejo en cuanto se anula un pago o se sube otro
-- soporte. Con el hash, cuantos pagos comparte un comprobante se cuenta al
-- leerlo y siempre es cierto.
--
-- Y NO se rechaza el repetido a proposito: un negocio que paga a cinco
-- personas en UNA transferencia tiene un solo soporte para los cinco, y eso es
-- correcto. Lo que hacia falta era que se NOTARA.
-- ---------------------------------------------------------------------
SET @hay4 := (SELECT COUNT(*) FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'employee_settlement'
                AND COLUMN_NAME = 'PaymentProofHash');
SET @ddl4 := IF(@hay4 = 0,
    'ALTER TABLE employee_settlement
       ADD COLUMN PaymentProofHash CHAR(64) NULL COMMENT ''SHA-256 del fichero del comprobante'' AFTER PaymentProofUrl,
       ADD KEY idx_es_proof_hash (BusinessId, PaymentProofHash)',
    'DO 0');
PREPARE stmt4 FROM @ddl4; EXECUTE stmt4; DEALLOCATE PREPARE stmt4;
