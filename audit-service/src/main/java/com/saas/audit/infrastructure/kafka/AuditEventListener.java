package com.saas.audit.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.audit.application.service.AuditService;
import com.saas.common.events.EventEnvelope;
import com.saas.common.events.EventTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Listener del topic dedicado {@code audit.events}. Grupo propio
 * ({@code audit-recorder}) para no competir con otros consumidores.
 *
 * Idempotencia en dos capas: Redis (rapida) + unicidad de EventId en BD.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditService auditService;
    private final ProcessedEventCache dedup;
    private final ObjectMapper mapper;

    @KafkaListener(
            topics = "${saas.outbox.audit-topic:audit.events}",
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
            log.error("Mensaje de auditoria no deserializable - DESCARTADO. partition={} offset={}",
                    partition, offset, ex);
            ack.acknowledge();
            return;
        }

        if (!EventTypes.AUDIT_RECORDED.equals(envelope.getType())) {
            ack.acknowledge();
            return;
        }

        if (!dedup.markIfFirst(envelope.getEventId())) {
            ack.acknowledge();
            return;
        }

        try {
            auditService.record(envelope);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Error registrando auditoria partition={} offset={} eventId={}: {}",
                    partition, offset, envelope.getEventId(), ex.getMessage(), ex);
            throw new RuntimeException(ex); // sin ack -> Kafka reentrega
        }
    }
}
