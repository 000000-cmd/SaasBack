package com.saas.search.infrastructure.kafka.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.common.events.EventEnvelope;
import com.saas.common.events.EventTypes;
import com.saas.search.domain.document.UserDocument;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import com.saas.search.infrastructure.kafka.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Indexa eventos {@code user.*} en Elasticsearch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventHandler implements EventHandler {

    private static final Set<String> SUPPORTED = Set.of(
            EventTypes.USER_CREATED,
            EventTypes.USER_UPDATED,
            EventTypes.USER_DELETED,
            EventTypes.USER_ROLES_CHANGED
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
        IndexCoordinates index = IndexCoordinates.of(indexNames.users());

        // user.deleted: borrar del indice
        if (EventTypes.USER_DELETED.equals(envelope.getType())) {
            String id = envelope.getAggregateId().toString();
            ops.delete(id, index);
            log.info("UserDocument eliminado: id={}", id);
            return;
        }

        // user.created / updated / roles.changed: upsert
        try {
            UserDocument doc = mapper.treeToValue(envelope.getPayload(), UserDocument.class);
            doc.setId(envelope.getAggregateId().toString());
            doc.setBusinessId(envelope.getBusinessId());
            doc.setUpdatedAt(envelope.getOccurredAt());
            doc.setDocVersion(envelope.getOccurredAt().toEpochMilli());

            // En user.created seteamos createdAt; en updated/roles_changed
            // dejamos lo que ya tenia el documento previo (Spring lo merge-a)

            if (EventTypes.USER_CREATED.equals(envelope.getType())) {
                doc.setCreatedAt(envelope.getOccurredAt());
            }

            ops.save(doc, index);
            log.info("UserDocument indexado: id={} type={}",
                    envelope.getAggregateId(), envelope.getType());

        } catch (Exception ex) {
            log.error("Error indexando UserDocument id={} type={}: {}",
                    envelope.getAggregateId(), envelope.getType(), ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }

    }
}
