package com.saas.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherImpl implements OutboxPublisher {

    private final OutboxEventRepository repo;
    private final ObjectMapper mapper;

    @Override
    public void publish(String eventType,
                        UUID businessId,
                        String aggregateType,
                        UUID aggregateId,
                        Object payload) {
        try {
            String payloadJson = mapper.writeValueAsString(payload);
            OutboxEvent event =OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .eventId(UUID.randomUUID())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .version(1)
                    .businessId(businessId)
                    .payload(payloadJson)
                    .status(OutboxEventStatus.PENDING)
                    .retries(0)
                    .createdAt(Instant.now())
                    .build();

            repo.save(event);
            log.debug("Outbox event encolado: type={} aggregateId={} eventId={}",
                    eventType, aggregateId, event.getEventId());

        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Error serializando payload del evento " + eventType + ": " + ex.getMessage(), ex);
        }
    }
}
