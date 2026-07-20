package com.saas.search.infrastructure.kafka.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.common.events.EventEnvelope;
import com.saas.common.events.EventTypes;
import com.saas.search.domain.document.EmployeeBalanceDocument;
import com.saas.search.infrastructure.elasticsearch.IndexNames;
import com.saas.search.infrastructure.kafka.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Proyecta el saldo por cobrar del empleado al indice {@code employee_balances}.
 * Escucha los eventos emitidos por finance ({@code finance.balance.*}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeBalanceEventHandler implements EventHandler {

    private static final Set<String> SUPPORTED = Set.of(
            EventTypes.FINANCE_BALANCE_UPDATED,
            EventTypes.FINANCE_BALANCE_DELETED
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
        IndexCoordinates index = IndexCoordinates.of(indexNames.employeeBalances());

        if (EventTypes.FINANCE_BALANCE_DELETED.equals(envelope.getType())) {
            ops.delete(envelope.getAggregateId().toString(), index);
            return;
        }

        try {
            EmployeeBalanceDocument doc = mapper.treeToValue(envelope.getPayload(), EmployeeBalanceDocument.class);
            doc.setId(envelope.getAggregateId().toString());
            doc.setBusinessId(envelope.getBusinessId());
            doc.setUpdatedAt(envelope.getOccurredAt());
            if (doc.getCreatedAt() == null) doc.setCreatedAt(envelope.getOccurredAt());
            doc.setDocVersion(envelope.getOccurredAt().toEpochMilli());
            ops.save(doc, index);
        } catch (Exception ex) {
            log.error("EmployeeBalanceEventHandler error id={}: {}", envelope.getAggregateId(), ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }
}
