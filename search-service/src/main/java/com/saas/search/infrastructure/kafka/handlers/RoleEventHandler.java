package com.saas.search.infrastructure.kafka.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.common.events.EventEnvelope;
import com.saas.common.events.EventTypes;
import com.saas.search.domain.document.RoleDocument;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import com.saas.search.infrastructure.kafka.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleEventHandler implements EventHandler {

    private static final Set<String> SUPPORTED = Set.of(
            EventTypes.ROLE_CREATED,
            EventTypes.ROLE_UPDATED,
            EventTypes.ROLE_DELETED
    );

    private final ElasticsearchOperations ops;
    private final ObjectMapper mapper;
    private final IndexNames indexNames;

    @Override
    public boolean supports(String eventType) {
        return SUPPORTED.contains(eventType);
    }

    @Override
    public void handle(EventEnvelope envelope) {
        IndexCoordinates index = IndexCoordinates.of(indexNames.roles());

        if (EventTypes.ROLE_DELETED.equals(envelope.getType())) {
            String id = envelope.getAggregateId().toString();
            ops.delete(id, index);
            return;
        }

        try {
            RoleDocument doc = mapper.treeToValue(envelope.getPayload(), RoleDocument.class);
            doc.setId(envelope.getAggregateId().toString());
            doc.setBusinessId(envelope.getBusinessId());
            doc.setUpdatedAt(envelope.getOccurredAt());
            doc.setDocVersion(envelope.getOccurredAt().toEpochMilli());

            if (EventTypes.ROLE_CREATED.equals(envelope.getType())) {
                doc.setCreatedAt(envelope.getOccurredAt());
            }

            ops.save(doc, index);
            log.info("RoleDocument indexado: id={} type={}",
                    envelope.getAggregateId(), envelope.getType());

        } catch (Exception ex) {
            log.error("Error indexando RoleDocument id={} type={}: {}",
                    envelope.getAggregateId(), envelope.getType(), ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }
}
