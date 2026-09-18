-- ---------------------------------------------------------------------------
-- Avisos de la agenda: confirmación, recordatorio y cancelación.
--
-- Hasta ahora, quien reservaba en la web pública veía su código en pantalla y
-- nada más: si cerraba la pestaña, no le quedaba constancia de nada. Estas
-- tres notificaciones son lo que convierte una reserva en algo que el cliente
-- tiene en la mano.
--
-- Va en events-service (esquema saas_events), que es quien resuelve plantillas
-- y despacha. El negocio solo publica al outbox el hecho y sus parámetros.
--
-- Se aplica a mano en el entorno de desarrollo Y vive en V1: editar V1 no
-- aplica nada sobre un esquema ya migrado, y recrear el esquema por un aviso
-- costaría las cuentas de prueba.
--
-- La identidad de una notificación es su CÓDIGO, no un UUID escrito a mano.
-- Sembrar con ids fijos + INSERT IGNORE fue un error real: el id elegido ya
-- era de otra notificación, la fila NO se insertó —en silencio, que es lo
-- peor— y dos plantillas quedaron colgando de la notificación equivocada. Por
-- eso aquí se comprueba por Code y el id lo pone la base.
-- ---------------------------------------------------------------------------

USE saas_events;

INSERT INTO notification_parameter (Id, Code, Name, Description, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), d.Code, d.Name, d.Descripcion, TRUE, TRUE, NOW(6), NOW(6)
FROM (
    SELECT 'HORA'   AS Code, 'Hora de la cita' AS Name,
           'La hora local del negocio, ya formateada ("09:00").' AS Descripcion
    UNION ALL
    SELECT 'MOTIVO', 'Motivo',
           'Por que se cancelo, tal y como lo escribio quien cancelo.'
) d
WHERE NOT EXISTS (SELECT 1 FROM notification_parameter p WHERE p.Code = d.Code);

INSERT INTO notification (Id, Code, Name, Description, IsGlobal, Enabled, Visible, AuditDate, CreatedDate)
SELECT UUID(), d.Code, d.Name, d.Descripcion, FALSE, TRUE, TRUE, NOW(6), NOW(6)
FROM (
    SELECT 'APPOINTMENT_CONFIRMED' AS Code, 'Cita confirmada' AS Name,
           'Le confirma al cliente su cita con el codigo para consultarla o cancelarla.' AS Descripcion
    UNION ALL
    SELECT 'APPOINTMENT_REMINDER', 'Recordatorio de cita',
           'Le recuerda al cliente su cita antes de la hora. Cuantas horas antes lo decide cada negocio.'
    UNION ALL
    SELECT 'APPOINTMENT_CANCELLED', 'Cita cancelada',
           'Avisa al cliente de que su cita ya no esta en pie, y por que.'
) d
WHERE NOT EXISTS (SELECT 1 FROM notification n WHERE n.Code = d.Code);

-- ---------------------------------------------------------------------
-- Plantillas.
--
-- Cada canal dice lo mismo con el espacio que tiene. El dato que NO puede
-- faltar en ninguno es el CÓDIGO: es lo único que le permite al cliente
-- volver a su cita sin tener cuenta.
-- ---------------------------------------------------------------------
-- La notificacion se enlaza POR CODIGO. Con el id escrito a mano, si ese id ya
-- era de otra cosa la plantilla acaba colgando de la notificacion equivocada y
-- nada avisa: se descubre cuando llega el mensaje que no era.
INSERT IGNORE INTO notification_template
    (Id, Code, Name, TypeCode, Subject, Body, NotificationId, Enabled, Visible, AuditDate, CreatedDate)
VALUES
(UUID(), 'APPOINTMENT_CONFIRMED_EMAIL', 'Cita confirmada (correo)', 'EMAIL',
 'Tu cita en {{NEGOCIO}} · {{FECHA}} {{HORA}}',
 CONCAT('<div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;background:#f8f9fa;padding:32px 16px">',
        '<div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e1e3e4">',
        '<div style="padding:32px 32px 24px">',
          '<p style="margin:0;font-size:11px;letter-spacing:.18em;text-transform:uppercase;color:#8204be;font-weight:700">Tu cita está confirmada</p>',
          '<p style="margin:12px 0 0;font-size:26px;line-height:1.2;font-weight:700;color:#151c27">{{FECHA}} a las {{HORA}}</p>',
          '<p style="margin:8px 0 0;font-size:15px;color:#5f5e5e">{{SERVICIO}} · con {{EMPLEADO}}</p>',
        '</div>',
        '<div style="padding:0 32px 8px">',
          '<div style="background:#f8f2ff;border-radius:12px;padding:20px 24px;text-align:center">',
            '<p style="margin:0;font-size:11px;letter-spacing:.16em;text-transform:uppercase;color:#7000a6;font-weight:700">Tu código</p>',
            '<p style="margin:8px 0 0;font-size:30px;font-weight:700;letter-spacing:.18em;color:#8204be">{{CODIGO}}</p>',
            '<p style="margin:10px 0 0;font-size:12px;color:#5f5e5e">Guárdalo: con él consultas o cancelas tu cita sin crear ninguna cuenta.</p>',
          '</div>',
        '</div>',
        '<div style="padding:24px 32px 32px;text-align:center">',
          '<a href="{{LINK}}" style="display:inline-block;background:#8204be;color:#ffffff;text-decoration:none;',
             'font-size:14px;font-weight:600;padding:14px 34px;border-radius:999px">Ver o cancelar mi cita</a>',
          '<p style="margin:20px 0 0;font-size:12px;line-height:1.6;color:#807384">',
            'Si no vas a poder venir, cancélala con tiempo: así otra persona puede tomar la hora.',
          '</p>',
        '</div>',
        '<div style="padding:18px 32px;border-top:1px solid #e1e3e4;background:#f9f9ff">',
          '<p style="margin:0;font-size:10px;letter-spacing:.14em;text-transform:uppercase;color:#807384">{{NEGOCIO}}</p>',
        '</div>',
        '</div></div>'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_CONFIRMED'), TRUE, TRUE, NOW(6), NOW(6)),

(UUID(), 'APPOINTMENT_CONFIRMED_WHATSAPP', 'Cita confirmada (WhatsApp)', 'WHATSAPP',
 'Tu cita en {{NEGOCIO}}',
 CONCAT('¡Listo, {{CLIENTE}}! 🗓️\n\nTu cita en *{{NEGOCIO}}* quedó confirmada.\n\n',
        '*Cuándo:* {{FECHA}} a las {{HORA}}\n*Qué:* {{SERVICIO}}\n*Con:* {{EMPLEADO}}\n\n',
        'Tu código es *{{CODIGO}}*. Con él puedes ver o cancelar tu cita aquí:\n{{LINK}}'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_CONFIRMED'), TRUE, TRUE, NOW(6), NOW(6)),

(UUID(), 'APPOINTMENT_CONFIRMED_SMS', 'Cita confirmada (SMS)', 'SMS',
 'Tu cita en {{NEGOCIO}}',
 CONCAT('{{NEGOCIO}}: tu cita quedó para el {{FECHA}} a las {{HORA}} ({{SERVICIO}}). ',
        'Código {{CODIGO}}. Consúltala o cancélala: {{LINK}}'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_CONFIRMED'), TRUE, TRUE, NOW(6), NOW(6)),

-- El recordatorio no repite todo: quien lo lee ya sabe qué pidió. Dice cuándo
-- es y da la salida para cancelar, que es la acción que de verdad hace falta a
-- estas alturas.
(UUID(), 'APPOINTMENT_REMINDER_WHATSAPP', 'Recordatorio (WhatsApp)', 'WHATSAPP',
 'Recordatorio de tu cita',
 CONCAT('Hola {{CLIENTE}} 👋\n\nTe recordamos tu cita en *{{NEGOCIO}}*:\n\n',
        '*{{FECHA}} a las {{HORA}}*\n{{SERVICIO}} · con {{EMPLEADO}}\n\n',
        '¿No vas a poder? Cancélala aquí para liberar la hora:\n{{LINK}}'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_REMINDER'), TRUE, TRUE, NOW(6), NOW(6)),

(UUID(), 'APPOINTMENT_REMINDER_EMAIL', 'Recordatorio (correo)', 'EMAIL',
 'Recordatorio: tu cita en {{NEGOCIO}} es el {{FECHA}}',
 CONCAT('<div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;background:#f8f9fa;padding:32px 16px">',
        '<div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:16px;padding:32px;border:1px solid #e1e3e4">',
          '<p style="margin:0;font-size:11px;letter-spacing:.18em;text-transform:uppercase;color:#8204be;font-weight:700">Te esperamos</p>',
          '<p style="margin:12px 0 0;font-size:24px;line-height:1.2;font-weight:700;color:#151c27">{{FECHA}} a las {{HORA}}</p>',
          '<p style="margin:8px 0 0;font-size:15px;color:#5f5e5e">{{SERVICIO}} · con {{EMPLEADO}} · {{NEGOCIO}}</p>',
          '<p style="margin:24px 0 0;font-size:13px;line-height:1.6;color:#5f5e5e">',
            'Si no vas a poder venir, cancélala con tu código <strong>{{CODIGO}}</strong> ',
            'para que otra persona pueda tomar la hora.',
          '</p>',
          '<div style="margin-top:24px">',
            '<a href="{{LINK}}" style="display:inline-block;background:#8204be;color:#ffffff;text-decoration:none;',
               'font-size:14px;font-weight:600;padding:12px 28px;border-radius:999px">Ver mi cita</a>',
          '</div>',
        '</div></div>'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_REMINDER'), TRUE, TRUE, NOW(6), NOW(6)),

-- Cancelar SIEMPRE dice por qué, aunque el motivo venga vacío: un aviso de que
-- algo se cayó sin explicación deja al cliente llamando por teléfono.
(UUID(), 'APPOINTMENT_CANCELLED_WHATSAPP', 'Cita cancelada (WhatsApp)', 'WHATSAPP',
 'Tu cita quedó cancelada',
 CONCAT('Hola {{CLIENTE}}:\n\nTu cita en *{{NEGOCIO}}* del {{FECHA}} a las {{HORA}} quedó cancelada.\n\n',
        '*Motivo:* {{MOTIVO}}\n\nCuando quieras puedes reservar otra hora aquí:\n{{LINK}}'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_CANCELLED'), TRUE, TRUE, NOW(6), NOW(6)),

(UUID(), 'APPOINTMENT_CANCELLED_EMAIL', 'Cita cancelada (correo)', 'EMAIL',
 'Tu cita en {{NEGOCIO}} del {{FECHA}} quedó cancelada',
 CONCAT('<div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;background:#f8f9fa;padding:32px 16px">',
        '<div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:16px;padding:32px;border:1px solid #e1e3e4">',
          '<p style="margin:0;font-size:11px;letter-spacing:.18em;text-transform:uppercase;color:#7a2c2c;font-weight:700">Cita cancelada</p>',
          '<p style="margin:12px 0 0;font-size:22px;line-height:1.3;font-weight:700;color:#151c27">{{FECHA}} a las {{HORA}}</p>',
          '<p style="margin:8px 0 0;font-size:15px;color:#5f5e5e">{{SERVICIO}} · {{NEGOCIO}}</p>',
          '<div style="margin-top:20px;background:#fdf2f2;border-radius:12px;padding:16px 20px">',
            '<p style="margin:0;font-size:11px;letter-spacing:.14em;text-transform:uppercase;color:#7a2c2c;font-weight:700">Motivo</p>',
            '<p style="margin:6px 0 0;font-size:14px;color:#151c27">{{MOTIVO}}</p>',
          '</div>',
          '<div style="margin-top:24px">',
            '<a href="{{LINK}}" style="display:inline-block;background:#8204be;color:#ffffff;text-decoration:none;',
               'font-size:14px;font-weight:600;padding:12px 28px;border-radius:999px">Reservar otra hora</a>',
          '</div>',
        '</div></div>'),
 (SELECT Id FROM notification WHERE Code = 'APPOINTMENT_CANCELLED'), TRUE, TRUE, NOW(6), NOW(6));
