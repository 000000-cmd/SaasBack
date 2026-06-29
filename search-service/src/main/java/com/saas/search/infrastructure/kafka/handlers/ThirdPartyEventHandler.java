package com.saas.search.infrastructure.kafka.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.common.events.EventEnvelope;
import com.saas.common.events.EventTypes;
import com.saas.search.domain.document.ThirdPartyDocument;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import com.saas.search.infrastructure.kafka.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Indexa Terceros en Elasticsearch a partir de eventos {@code thirdparty.*}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThirdPartyEventHandler implements EventHandler {

    private static final Set<String> SUPPORTED = Set.of(
            EventTypes.THIRDPARTY_CREATED,
            EventTypes.THIRDPARTY_UPDATED,
            EventTypes.THIRDPARTY_DELETED
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
        IndexCoordinates index = IndexCoordinates.of(indexNames.thirdParties());

        if (EventTypes.THIRDPARTY_DELETED.equals(envelope.getType())) {
            ops.delete(envelope.getAggregateId().toString(), index);
            return;
        }

        try {
            ThirdPartyDocument doc = mapper.treeToValue(envelope.getPayload(), ThirdPartyDocument.class);
            doc.setId(envelope.getAggregateId().toString());
            doc.setBusinessId(envelope.getBusinessId());
            doc.setUpdatedAt(envelope.getOccurredAt());
            doc.setDocVersion(envelope.getOccurredAt().toEpochMilli());

            if (EventTypes.THIRDPARTY_CREATED.equals(envelope.getType())) {
                doc.setCreatedAt(envelope.getOccurredAt());
            }

            ops.save(doc, index);
            log.info("ThirdPartyDocument indexado: id={} type={}",
                    envelope.getAggregateId(), envelope.getType());

        } catch (Exception ex) {
            log.error("Error indexando ThirdPartyDocument id={} type={}: {}",
                    envelope.getAggregateId(), envelope.getType(), ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }
}
