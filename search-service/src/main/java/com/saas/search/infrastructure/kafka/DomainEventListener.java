package com.saas.search.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.common.events.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Listener principal del topic {@code domain.events}.
 *
 * Flujo por mensaje:
 *
 *   Deserializa el JSON al {@link EventEnvelope}.
 *   Verifica idempotencia: si {@code eventId} ya fue procesado, descarta.
 *   Busca un {@link EventHandler} que soporte el {@code type} y lo invoca.
 *   Si todo OK, confirma offset (ack manual).
 *
 *
 * Si algo lanza excepcion, el offset NO se confirma y el ErrorHandler
 * (con backoff de 2s, 3 retries) reentrega.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventListener {

    private final List<EventHandler> handlers;
    private final ObjectMapper mapper;
    private final ProcessedEventCache dedup;

    @KafkaListener(
            topics = "${saas.outbox.topic:domain.events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(@Payload String json,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset,
                          Acknowledgment ack) {
        EventEnvelope envelope;
        try {
            envelope = mapper.readValue(json, EventEnvelope.class);
        } catch (Exception ex) {
            log.error("Mensaje no deserializable - DESCARTADO. partition={} offset={} json='{}'",
                    partition, offset, json, ex);
            // Acknowledge para NO reintentar: el mensaje esta corrupto, retry no ayuda.
            ack.acknowledge();
            return;
        }

        // Idempotencia: si ya procesamos este eventId, descartar
        if(!dedup.markIfFirst(envelope.getEventId())){
            ack.acknowledge();
            return;
        }

        try {
            int dispatched = 0;

            for  (EventHandler handler : handlers) {
                if(handler.supports(envelope.getType())){
                    handler.handle(envelope);
                    dispatched++;
                }
            }

            if (dispatched == 0) {
                log.debug("Sin handler para type={} (ignorado, no es un error)",
                        envelope.getType());
            }

            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Error procesando evento partition={} offset={} type={}: {}",
                    partition, offset, envelope.getType(), ex.getMessage(), ex);
            // NO acknowledge → ErrorHandler reentregara
            throw new RuntimeException(ex);
        }
    }
}
