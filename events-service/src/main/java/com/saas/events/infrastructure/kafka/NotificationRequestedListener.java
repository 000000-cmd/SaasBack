package com.saas.events.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.common.events.EventEnvelope;
import com.saas.common.events.EventTypes;
import com.saas.events.application.service.DispatchService;
import com.saas.events.domain.model.Attachment;
import com.saas.events.infrastructure.client.ResendClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Escucha domain.events y actua solo sobre notification.requested. Convive con
 * AuditEventListener en la misma JVM: distinto topic, distinto grupo, distinto
 * contenedor de hilos.
 *
 * Comparte {@link ProcessedEventCache} (mismo bean, mismo prefijo de Redis) con
 * AuditEventListener. Esto es seguro: cada evento nace con un eventId propio
 * (UUID.randomUUID() en OutboxPublisherImpl) y OutboxRelay lo enruta a UN solo
 * topic segun el prefijo del eventType ("audit." -> audit.events, el resto ->
 * domain.events). Un mismo eventId nunca aparece en los dos topics, asi que
 * compartir el keyspace de dedup entre listeners no puede descartar un evento
 * que le correspondia a este.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestedListener {

    private final DispatchService dispatch;
    private final ObjectMapper mapper;
    private final ProcessedEventCache dedup;

    @KafkaListener(
            topics = "${saas.outbox.topic:domain.events}",
            groupId = "${saas.notification.group-id:notification-sender}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(@Payload String json,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset,
                          Acknowledgment ack) {
        EventEnvelope envelope;
        try {
            envelope = mapper.readValue(json, EventEnvelope.class);
        } catch (Exception ex) {
            log.error("Mensaje no deserializable - DESCARTADO. partition={} offset={}", partition, offset, ex);
            ack.acknowledge();   // corrupto: reintentar no ayuda
            return;
        }

        if (!EventTypes.NOTIFICATION_REQUESTED.equals(envelope.getType())) {
            ack.acknowledge();
            return;
        }

        if (!dedup.markIfFirst(envelope.getEventId())) {
            ack.acknowledge();
            return;
        }

        try {
            JsonNode p = envelope.getPayload();
            if (p == null || !p.hasNonNull("notificationCode")) {
                log.warn("notification.requested sin notificationCode; se descarta. eventId={}",
                        envelope.getEventId());
                ack.acknowledge();
                return;
            }

            List<String> to = new ArrayList<>();
            if (p.has("to") && p.get("to").isArray()) {
                p.get("to").forEach(n -> to.add(n.asText()));
            }

            Map<String, String> data = new LinkedHashMap<>();
            if (p.has("data") && p.get("data").isObject()) {
                p.get("data").fields().forEachRemaining(e ->
                        data.put(e.getKey(), e.getValue().isNull() ? null : e.getValue().asText()));
            }

            // Si quien publica dice de QUIEN es la notificacion, ademas de
            // enviarla se le escribe en su bandeja: es lo que leen la campana de
            // la web y la lista del movil. Sin dueno no se puede mostrar a nadie.
            UUID thirdPartyId = uuidOrNull(p.path("thirdPartyId").asText(null));

            // Canales permitidos por quien publica. Sin ellos salen todos, que
            // es como se comportaba antes de que existiera el campo.
            java.util.Set<String> channels = new java.util.LinkedHashSet<>();
            if (p.has("channels") && p.get("channels").isArray()) {
                p.get("channels").forEach(n -> channels.add(n.asText()));
            }

            // El negocio viene en el sobre, no en el payload: es la misma pieza
            // que usa el aislamiento, y es la que decide desde QUE numero de
            // WhatsApp sale el mensaje.
            dispatch.dispatch(p.get("notificationCode").asText(),
                    ResendClient.cleanRecipients(to), data, envelope.getEventId(),
                    thirdPartyId, attachmentOf(p), channels, envelope.getBusinessId());
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Error enviando notificacion. partition={} offset={}", partition, offset, ex);
            // NO acknowledge -> Kafka reentrega; uq_log_event_template evita el duplicado.
            throw new RuntimeException(ex);
        }
    }

    /**
     * Adjunto opcional del evento. Viaja en base64 dentro del payload porque el
     * fichero lo construye quien conoce el dominio (finance genera el extracto y
     * lo cifra) y este servicio solo lo transporta. Son unas decenas de KB: cabe
     * de sobra en un mensaje de Kafka.
     */
    /**
     * Un id mal formado no puede tumbar el envio: sin bandeja se pierde una
     * comodidad, pero al no acusar recibo Kafka reentregaria el mensaje para
     * siempre y el mensaje NUNCA saldria.
     */
    private static UUID uuidOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("thirdPartyId invalido en notification.requested: {}", raw);
            return null;
        }
    }

    private static Attachment attachmentOf(JsonNode p) {
        JsonNode a = p.get("attachment");
        if (a == null || !a.isObject()) return null;
        Attachment attachment = new Attachment(
                a.path("filename").asText(null), a.path("contentBase64").asText(null));
        return attachment.isUsable() ? attachment : null;
    }
}
