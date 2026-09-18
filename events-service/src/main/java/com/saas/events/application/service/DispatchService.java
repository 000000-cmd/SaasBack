package com.saas.events.application.service;

import com.saas.events.domain.model.*;
import com.saas.events.domain.port.out.*;
import com.saas.events.infrastructure.channel.ChannelSender;
import com.saas.events.infrastructure.realtime.InboxStreamHub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private final INotificationRepositoryPort notifications;
    private final INotificationTemplateRepositoryPort templates;
    private final INotificationLogRepositoryPort logs;
    private final INotificationInboxRepositoryPort inbox;
    private final NotificationSettingService settingsService;
    private final TemplateRenderer renderer;
    private final List<ChannelSender> senders;
    private final InboxStreamHub stream;

    /**
     * Envio sin destinatario identificado: no se puede escribir bandeja porque
     * no se sabe de quien es. Lo usa el POST directo y las pruebas del panel.
     */
    @Transactional
    public List<NotificationLog> dispatch(String notificationCode,
                                          List<String> recipients,
                                          Map<String, String> data,
                                          UUID eventId) {
        return dispatch(notificationCode, recipients, data, eventId, null);
    }

    /**
     * Resuelve la notificacion, renderiza cada plantilla activa y la envia a
     * cada destinatario. Devuelve una entrada de bitacora por combinacion
     * plantilla x destinatario.
     *
     * @param thirdPartyId a quien pertenece el envio. Si viene, se escribe
     *        ademas una entrada en su BANDEJA por cada plantilla — que es lo que
     *        leen la campana de la web y la lista del movil. Si no viene, no se
     *        escribe: una notificacion sin dueno no se le puede mostrar a nadie.
     */
    @Transactional
    public List<NotificationLog> dispatch(String notificationCode,
                                          List<String> recipients,
                                          Map<String, String> data,
                                          UUID eventId,
                                          UUID thirdPartyId) {
        return dispatch(notificationCode, recipients, data, eventId, thirdPartyId, null);
    }

    /**
     * Igual que el anterior, pero con un ADJUNTO que se pasa tal cual al canal.
     *
     * <p>Este servicio no sabe que hay dentro del fichero ni tiene por que: lo
     * construye quien pide el envio (finance genera el extracto de nomina y lo
     * cifra con el documento del empleado). Manteniendolo opaco, events-service
     * sigue sin conocer ningun dominio.</p>
     */
    @Transactional
    public List<NotificationLog> dispatch(String notificationCode,
                                          List<String> recipients,
                                          Map<String, String> data,
                                          UUID eventId,
                                          UUID thirdPartyId,
                                          Attachment attachment) {
        return dispatch(notificationCode, recipients, data, eventId, thirdPartyId, attachment, null);
    }

    /**
     * Igual, pero limitando POR QUE CANALES puede salir.
     *
     * <p>Quien publica sabe cosas que este servicio no puede saber: si el
     * cliente aceptó recibir WhatsApp, si se dio de baja, o si el negocio tiene
     * el canal apagado. Sin esta restricción la única forma de expresarlo sería
     * duplicar la notificación —una por canal—, y entonces cambiar un texto
     * obligaría a cambiarlo en varios sitios.</p>
     *
     * @param channels códigos de tipo permitidos ({@code EMAIL}, {@code WHATSAPP}…).
     *        Nulo o vacío = todos los que tenga la notificación, como siempre.
     */
    @Transactional
    public List<NotificationLog> dispatch(String notificationCode,
                                          List<String> recipients,
                                          Map<String, String> data,
                                          UUID eventId,
                                          UUID thirdPartyId,
                                          Attachment attachment,
                                          java.util.Set<String> channels) {
        return dispatch(notificationCode, recipients, data, eventId, thirdPartyId,
                attachment, channels, null);
    }

    /**
     * Igual, diciendo ademas DE QUE NEGOCIO sale.
     *
     * <p>Lo necesita WhatsApp: cada negocio envia desde SU numero, no desde el
     * de la plataforma. Los demas canales lo ignoran. Va nulo cuando el aviso
     * no es de ningun negocio (un lanzamiento global, por ejemplo).</p>
     */
    @Transactional
    public List<NotificationLog> dispatch(String notificationCode,
                                          List<String> recipients,
                                          Map<String, String> data,
                                          UUID eventId,
                                          UUID thirdPartyId,
                                          Attachment attachment,
                                          java.util.Set<String> channels,
                                          UUID businessId) {
        List<NotificationLog> written = new ArrayList<>();
        NotificationSetting settings = settingsService.get();

        Notification notification = notifications.findByCode(notificationCode).orElse(null);
        if (notification == null || Boolean.FALSE.equals(notification.getEnabled())) {
            log.warn("Notificacion '{}' inexistente o deshabilitada; no se envia nada", notificationCode);
            return written;
        }

        boolean acotado = channels != null && !channels.isEmpty();
        List<NotificationTemplate> tpls = templates.findByNotificationId(notification.getId()).stream()
                .filter(t -> Boolean.TRUE.equals(t.getEnabled()))
                .filter(t -> !acotado || channels.contains(t.getTypeCode()))
                .toList();
        if (tpls.isEmpty()) {
            log.warn("Notificacion '{}' sin plantillas activas{}", notificationCode,
                    acotado ? " para los canales " + channels : "");
            return written;
        }

        for (NotificationTemplate tpl : tpls) {
            ChannelType channel = ChannelType.from(tpl.getTypeCode());

            // BANDEJA: una entrada por plantilla, no por destinatario. El cuerpo
            // no depende del destinatario (sale de `data`), y la bandeja es de
            // la PERSONA, no de su correo o su telefono. Se escribe antes de
            // intentar el envio a proposito: que el proveedor falle no significa
            // que la notificacion no exista — significa que no salio por ahi.
            if (thirdPartyId != null) {
                NotificationInbox fila = inbox.save(NotificationInbox.builder()
                        .thirdPartyId(thirdPartyId)
                        .notificationCode(notification.getCode())
                        .templateCode(tpl.getCode())
                        .typeCode(tpl.getTypeCode())
                        .title(renderer.render(tpl.getSubject(), data, channel))
                        .body(renderer.render(tpl.getBody(), data, channel))
                        .build());
                avisarEnVivo(thirdPartyId, fila);
            }

            for (String rawRecipient : forChannel(channel, recipients)) {
                // Modo prueba: TODO se desvia, y el asunto lo dice. Es lo que
                // impide que desde desarrollo le llegue correo a un cliente real.
                boolean test = Boolean.TRUE.equals(settings.getTestMode());
                String recipient = test ? settings.getTestRecipient() : rawRecipient;
                if (recipient == null || recipient.isBlank()) {
                    written.add(write(tpl, notification, channel, rawRecipient, null,
                            SendStatus.SKIPPED, null,
                            test ? "Modo prueba sin dirección de pruebas configurada"
                                 : "Destinatario vacío", eventId));
                    continue;
                }

                String subject = renderer.render(tpl.getSubject(), data, channel);
                if (test && subject != null) subject = "[PRUEBA] " + subject;
                String body = renderer.render(tpl.getBody(), data, channel);

                List<String> missing = renderer.missingParameters(tpl.getBody(), data);
                if (!missing.isEmpty()) {
                    log.warn("Plantilla '{}': parametros sin valor {}", tpl.getCode(), missing);
                }

                if (!Boolean.TRUE.equals(settings.getSendingEnabled())) {
                    written.add(write(tpl, notification, channel, recipient, subject,
                            SendStatus.SKIPPED, null, "Envío apagado en la configuración", eventId));
                    continue;
                }

                ChannelSender sender = senders.stream()
                        .filter(s -> s.supports(channel)).findFirst().orElse(null);
                if (sender == null) {
                    written.add(write(tpl, notification, channel, recipient, subject,
                            SendStatus.NO_PROVIDER, null, "Canal desconocido: " + tpl.getTypeCode(), eventId));
                    continue;
                }

                ChannelSender.Outcome out = sender.send(settings, recipient, subject, body,
                        attachment, businessId);
                NotificationLog row = write(tpl, notification, channel, recipient, subject,
                        out.status(), out.providerMessageId(),
                        missing.isEmpty() ? out.error() : append(out.error(), "Sin valor: " + missing),
                        eventId);
                if (row != null) written.add(row);
            }
        }
        return written;
    }

    /**
     * Empuja la fila recien escrita a quien tenga la web o la app abierta.
     *
     * DESPUES DEL COMMIT, no antes: si se avisara dentro de la transaccion, el
     * cliente recibiria el aviso, iria a leer la bandeja y no encontraria la
     * fila todavia — o peor, la transaccion podria deshacerse despues y el
     * aviso quedaria contando algo que nunca ocurrio.
     */
    private void avisarEnVivo(UUID owner, NotificationInbox fila) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("id", String.valueOf(fila.getId()));
        payload.put("title", fila.getTitle());
        payload.put("body", fila.getBody());
        payload.put("notificationCode", fila.getNotificationCode());
        payload.put("typeCode", fila.getTypeCode());
        payload.put("createdDate", String.valueOf(fila.getCreatedDate()));

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            stream.publish(owner, "inbox", payload);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                stream.publish(owner, "inbox", payload);
            }
        });
    }

    /**
     * Los destinatarios que TIENEN SENTIDO para este canal.
     *
     * <p>Una notificacion puede traer varios destinos de distinta naturaleza: la
     * factura de un cliente va a su correo y a su telefono. Sin este filtro, la
     * plantilla de correo se "enviaba" tambien al numero y la de SMS a la
     * direccion — dos mensajes imposibles por cada uno bueno.</p>
     *
     * <p>La arroba es el unico criterio y es suficiente: un correo la lleva
     * siempre y un telefono no la lleva nunca. PUSH no se filtra porque ahi el
     * destinatario no es una direccion sino la persona, y sus dispositivos los
     * resuelve su propio canal.</p>
     */
    private static List<String> forChannel(ChannelType channel, List<String> recipients) {
        if (channel != ChannelType.EMAIL && channel != ChannelType.SMS && channel != ChannelType.WHATSAPP) {
            return recipients;
        }
        boolean wantEmail = channel == ChannelType.EMAIL;
        return recipients.stream()
                .filter(r -> r != null && r.contains("@") == wantEmail)
                .toList();
    }

    private static String append(String base, String extra) {
        return base == null || base.isBlank() ? extra : base + " | " + extra;
    }

    /**
     * Escribe la bitacora. Si choca contra uq_log_event_template significa que
     * este mismo evento ya se proceso para esta plantilla: Kafka reentrego el
     * mensaje. Se descarta en silencio, que es exactamente el comportamiento
     * deseado — no un error.
     */
    private NotificationLog write(NotificationTemplate tpl, Notification notification,
                                  ChannelType channel, String recipient, String subject,
                                  SendStatus status, String providerId, String error, UUID eventId) {
        NotificationLog row = NotificationLog.builder()
                .notificationCode(notification.getCode())
                .templateCode(tpl.getCode())
                .templateId(tpl.getId())
                .typeCode(tpl.getTypeCode())
                .recipient(recipient == null ? "(sin destinatario)" : recipient)
                .subject(subject)
                .status(status)
                .providerMessageId(providerId)
                .error(error)
                .eventId(eventId)
                .attemptCount(1)
                .sentAt(status == SendStatus.SENT ? LocalDateTime.now() : null)
                .build();
        try {
            return logs.saveNow(row);
        } catch (DataIntegrityViolationException dup) {
            log.debug("Evento {} ya procesado para la plantilla {}; se descarta", eventId, tpl.getCode());
            return null;
        }
    }
}
