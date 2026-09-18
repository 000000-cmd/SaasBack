-- =====================================================================
-- V2__1.0.0.sql  (events)
-- INVITACION A INSTALAR LA APP.
--
-- Va aparte de V1 porque llega despues, con el esquema ya creado y con
-- datos. Meterlo en V1 obligaria a recrear la base de eventos entera y
-- se llevaria por delante la bitacora de envios, que es justo lo que
-- nadie quiere perder. Al consolidar el esquema para produccion, este
-- bloque se mueve a V1 y este archivo desaparece.
--
-- El correo lleva colores literales A PROPOSITO. Es la excepcion
-- documentada del sistema de diseno: un cliente de correo no resuelve
-- variables CSS, asi que aqui no hay tema del inquilino que valga.
-- =====================================================================

INSERT INTO notification (Id, Code, Name, Description, IsGlobal, Enabled, Visible, AuditDate, CreatedDate)
SELECT '99995000-0000-0000-0000-000000000005', 'APP_INVITE', 'Invitación a instalar la app',
       'El dueño le manda a su equipo el enlace para instalar la app y entrar con su cuenta.',
       -- NO es global: se manda a personas concretas del negocio, nunca a toda
       -- la base. Marcarla global permitiria escribirle a todos los clientes.
       FALSE, TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification WHERE Code = 'APP_INVITE');

INSERT INTO notification_template
    (Id, Code, Name, TypeCode, Subject, Body, NotificationId, Enabled, Visible, AuditDate, CreatedDate)
SELECT '99996000-0000-0000-0000-000000000007', 'APP_INVITE_EMAIL', 'Invitación a la app (correo)', 'EMAIL',
       '{{NEGOCIO}} te invita a instalar la app',
       CONCAT(
         '<div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;background:#f4f5f7;padding:32px 16px">',
           '<div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e2e5ea">',
             '<div style="background:#131b2e;padding:28px">',
               '<p style="margin:0;color:#8fa3c8;font-size:12px;letter-spacing:.08em;text-transform:uppercase">Moda ERP</p>',
               '<p style="margin:6px 0 0;color:#ffffff;font-size:20px;font-weight:700">{{NEGOCIO}} te invita a su app</p>',
             '</div>',
             '<div style="padding:28px">',
               '<p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#2b3244">',
                 'Desde la app ves tus servicios del día, tu saldo a favor y cuándo te pagan. ',
                 'Entras con la misma cuenta que ya tienes.',
               '</p>',
               '<p style="margin:24px 0">',
                 '<a href="{{LINK}}" style="display:inline-block;background:#0084d2;color:#ffffff;',
                 'text-decoration:none;padding:13px 24px;border-radius:8px;font-weight:600;font-size:15px">',
                 'Descargar la app</a>',
               '</p>',
               -- El enlace en texto ademas del boton: hay clientes de correo que
               -- no pintan el boton, y ahi el mensaje se quedaria sin salida.
               '<p style="margin:0 0 8px;font-size:13px;color:#697386">Si el botón no funciona, copia este enlace:</p>',
               '<p style="margin:0;font-size:13px;word-break:break-all"><a href="{{LINK}}" style="color:#0084d2">{{LINK}}</a></p>',
               '<hr style="border:0;border-top:1px solid #e2e5ea;margin:24px 0">',
               '<p style="margin:0;font-size:12px;line-height:1.5;color:#8a94a6">',
                 'Es una app para Android. Al abrir el archivo, el teléfono puede pedirte permiso ',
                 'para instalar aplicaciones de esta procedencia: es normal, se concede una vez.',
               '</p>',
             '</div>',
           '</div>',
         '</div>'),
       '99995000-0000-0000-0000-000000000005', TRUE, TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE Code = 'APP_INVITE_EMAIL');

-- No se crean parametros: NEGOCIO y LINK ya existen en V1 y son exactamente
-- lo que necesita esta plantilla. Inventar un {{ENLACE}} paralelo habria dejado
-- dos parametros para el mismo dato y una lista mas larga en el panel.
