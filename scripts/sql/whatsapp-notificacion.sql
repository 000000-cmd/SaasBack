-- ---------------------------------------------------------------------------
-- La respuesta del bot de WhatsApp.
--
-- Sale por el MISMO camino que el resto de avisos —outbox → Kafka →
-- events-service→ canal— y no llamando a Meta desde el webhook. Llamar desde
-- alli meteria una llamada de red dentro de la transaccion que acaba de crear
-- la cita: si Meta no responde, o la cita no se guarda o el mensaje no sale.
--
-- La plantilla es SOLO {{MENSAJE}} a proposito. El texto ya viene compuesto por
-- la conversacion, que es quien sabe en que paso va; darle aqui una estructura
-- fija obligaria a partir cada respuesta en trozos que no existen.
--
-- Se siembra por CODIGO, nunca por un id escrito a mano.
-- ---------------------------------------------------------------------------

USE saas_events;

INSERT INTO notification (Id, Code, Name, Description, IsGlobal, Enabled, Visible,
                          AuditDate, CreatedDate)
SELECT UUID(), 'WHATSAPP_REPLY', 'Respuesta del bot de WhatsApp',
       'Lo que el asistente contesta en la conversacion de reserva. El texto lo compone el flujo.',
       FALSE, TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification n WHERE n.Code = 'WHATSAPP_REPLY');

INSERT INTO notification_parameter (Id, Code, Name, Description, Enabled, Visible,
                                    AuditDate, CreatedDate)
SELECT UUID(), 'MENSAJE', 'Mensaje',
       'El texto ya compuesto por la conversacion.', TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification_parameter p WHERE p.Code = 'MENSAJE');

INSERT IGNORE INTO notification_template
    (Id, Code, Name, TypeCode, Subject, Body, NotificationId, Enabled, Visible,
     AuditDate, CreatedDate)
VALUES
(UUID(), 'WHATSAPP_REPLY_WHATSAPP', 'Respuesta del bot (WhatsApp)', 'WHATSAPP',
 'Reserva', '{{MENSAJE}}',
 (SELECT Id FROM notification WHERE Code = 'WHATSAPP_REPLY'), TRUE, TRUE, NOW(6), NOW(6));
