-- =====================================================================
-- EL WEBHOOK SE DA DE ALTA SOLO
-- ---------------------------------------------------------------------
-- Hasta ahora el dueño, despues de pegar sus credenciales, tenia que
-- VOLVER a Meta y pegar alli dos valores mas (la URL y la palabra del
-- apreton de manos) y suscribirse al campo `messages`. Ese es el paso
-- que mas se olvida, y olvidarlo no falla: el negocio queda "conectado"
-- enviando y sin recibir una sola cita.
--
-- Meta tiene un endpoint para hacerlo por API:
--   POST /{WABA_ID}/subscribed_apps
--        { override_callback_uri, verify_token }
-- que con el token del propio dueño suscribe su app a su cuenta de
-- WhatsApp Y apunta el webhook aqui, de una vez.
--
-- Hace falta el identificador de la CUENTA de WhatsApp Business (el
-- WABA id), que Meta enseña en la misma pantalla y justo encima del
-- identificador del numero. Un campo mas al pegar, dos pasos menos
-- despues.
--
-- WhatsappWebhookAt guarda CUANDO se consiguio. Nulo significa que no
-- se pudo y que al dueño hay que seguir enseñandole las instrucciones a
-- mano: es la diferencia entre "no hace falta que hagas nada" y "haz
-- esto o no recibiras citas", y no se puede adivinar.
-- =====================================================================

SET @hay := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = 'saas_db' AND TABLE_NAME = 'business_booking_policy'
               AND COLUMN_NAME = 'WhatsappWabaId');
SET @ddl := IF(@hay = 0,
    'ALTER TABLE business_booking_policy
       ADD COLUMN WhatsappWabaId VARCHAR(40) NULL COMMENT ''Id de la cuenta de WhatsApp Business (WABA) del negocio'' AFTER WhatsappPhoneId,
       ADD COLUMN WhatsappWebhookAt DATETIME(6) NULL COMMENT ''Cuando se dio de alta el webhook contra Meta. NULL = hay que hacerlo a mano'' AFTER WhatsappVerifyToken',
    'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
