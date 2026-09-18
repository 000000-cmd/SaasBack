-- ---------------------------------------------------------------------------
-- Cada negocio con SU numero de WhatsApp.
--
-- Hasta ahora las credenciales eran una sola para toda la plataforma
-- (`saas.whatsapp.*`), asi que todos los avisos salian del mismo numero. Para
-- que un negocio reciba citas en el suyo hacen falta sus propias credenciales.
--
-- EL TOKEN VA CIFRADO. Un volcado de la base con los tokens en claro deja a
-- cualquiera enviando mensajes en nombre de todos los negocios conectados: es
-- de los pocos secretos aqui que valen por si solos. Se guarda con AES-GCM y la
-- llave vive en la configuracion del servicio, no en la base — separadas, que
-- es lo unico que hace que cifrar sirva de algo.
--
-- El numero legible (`WhatsappDisplayPhone`) y el nombre verificado se guardan
-- porque los devuelve Meta al conectar y son lo que la pantalla ENSENA. Volver
-- a preguntarselos a Meta en cada carga seria una llamada de red para pintar
-- una etiqueta.
-- ---------------------------------------------------------------------------

USE saas_db;

SET @hay := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'business_booking_policy'
               AND COLUMN_NAME = 'WhatsappAccessToken');
SET @ddl := IF(@hay = 0,
    'ALTER TABLE business_booking_policy
       ADD COLUMN WhatsappAccessToken VARCHAR(2000) NULL COMMENT ''Token de la Cloud API, CIFRADO con AES-GCM'' AFTER WhatsappPhoneId,
       ADD COLUMN WhatsappDisplayPhone VARCHAR(32) NULL COMMENT ''El numero tal y como se lee (+57 300 123 4567)'' AFTER WhatsappAccessToken,
       ADD COLUMN WhatsappVerifiedName VARCHAR(160) NULL COMMENT ''Nombre verificado que Meta muestra al destinatario'' AFTER WhatsappDisplayPhone,
       ADD COLUMN WhatsappVerifiedAt DATETIME(6) NULL COMMENT ''Cuando se comprobo contra Meta que las credenciales sirven'' AFTER WhatsappVerifiedName',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
