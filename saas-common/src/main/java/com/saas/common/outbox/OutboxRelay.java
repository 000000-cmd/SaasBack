package com.saas.common.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.common.events.EventEnvelope;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "saas.outbox",
        name = "relay-enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    @Value("${spring.application.name}")
    private String producerName;

    @Value("${saas.outbox.topic:domain.events}")
    private String topic;

    @Value("${saas.outbox.batch-size:100}")
    private int batchSize;

    @Value("${saas.outbox.max-retries:5}")
    private int maxRetries;

    /**
     * Procesa un batch de eventos PENDING. La transaccion engloba
     */
    @Transactional
    @Scheduled(fixedDelayString = "${saas.outbox.poll-delay-ms:2000}")
    public void flush(){
        List<OutboxEvent> batch = repo.lockBatch(batchSize);
        if(batch.isEmpty()) return;

        log.debug("Outbox relay: procesando batch de {} eventos", batch.size());

        for(OutboxEvent event : batch){
            try {
                publish(event);
                event.setStatus(OutboxEventStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
            } catch (Exception ex) {
                event.setRetries(event.getRetries() + 1);
                event.setLastError(truncate(ex.getMessage(), 1000));

                if(event.getRetries() >= maxRetries){
                    event.setStatus(OutboxEventStatus.FAILED);
                    log.error("Outbox event FAILED tras {} retries: id={} type={}",
                            maxRetries, event.getId(), event.getEventType(), ex);
                } else {
                    log.warn("Outbox publish fallo (retry {}/{}): id={} type={} err={}",
                            event.getRetries(), maxRetries,
                            event.getId(), event.getEventType(), ex.getMessage());
                }
            }
        }
    }

    private void publish(OutboxEvent event) throws Exception {
        EventEnvelope envelope = toEnvelope(event);

        String key = (event.getBusinessId() != null
                ? event.getBusinessId()
                : event.getAggregateId()).toString();

        String json =  mapper.writeValueAsString(envelope);

        SendResult<String, String> result =
                kafka.send(topic, key, json).get(5, TimeUnit.SECONDS);

        log.debug("Outbox event publicado: type={} key={} partition={} offset={}",
                event.getEventType(),
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }


    private EventEnvelope toEnvelope(OutboxEvent e) throws Exception {
        JsonNode payload = mapper.readTree(e.getPayload());
        return EventEnvelope.builder()
                .eventId(e.getEventId())
                .type(e.getEventType())
                .version(e.getVersion())
                .businessId(e.getBusinessId())
                .aggregateId(e.getAggregateId())
                .aggregateType(e.getAggregateType())
                .occurredAt(e.getCreatedAt())
                .producer(producerName)
                .payload(payload)
                .build();
    }

    private static String truncate (String s, int max){
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

}
