-- ---------------------------------------------------------------------------
-- El webhook de CADA negocio.
--
-- Con credenciales pegadas, cada negocio tiene SU app de Meta, y Meta firma
-- cada entrega con el secreto de esa app. El webhook comprobaba la firma contra
-- un unico secreto de plataforma: cualquier negocio que conectara su numero
-- veria sus mensajes rechazados con 403 y nunca recibiria una cita.
--
-- El secreto de la app va CIFRADO por lo mismo que el token: con el se pueden
-- falsificar entregas hacia nosotros.
--
-- El verify token NO va cifrado a proposito: solo sirve para el apreton de
-- manos inicial, no autoriza nada, y hay que poder ENSENARSELO al dueno para
-- que lo pegue en Meta. Cifrar algo que se muestra en pantalla no protege de
-- nada y esconde que se puede mostrar.
-- ---------------------------------------------------------------------------

USE saas_db;

SET @hay := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'business_booking_policy'
               AND COLUMN_NAME = 'WhatsappAppSecret');
SET @ddl := IF(@hay = 0,
    'ALTER TABLE business_booking_policy
       ADD COLUMN WhatsappAppSecret VARCHAR(2000) NULL COMMENT ''Secreto de la app de Meta del negocio, CIFRADO'' AFTER WhatsappAccessToken,
       ADD COLUMN WhatsappVerifyToken VARCHAR(64) NULL COMMENT ''Palabra del apreton de manos del webhook. Se le ensena al dueno'' AFTER WhatsappAppSecret',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
