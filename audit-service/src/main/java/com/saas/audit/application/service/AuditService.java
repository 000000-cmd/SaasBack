package com.saas.audit.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.audit.application.dto.AuditLogResponse;
import com.saas.audit.domain.model.AuditLog;
import com.saas.audit.domain.repository.AuditLogRepository;
import com.saas.common.audit.AuditAction;
import com.saas.common.audit.AuditPayload;
import com.saas.common.dto.PagedResponse;
import com.saas.common.events.EventEnvelope;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repo;
    private final ObjectMapper mapper;

    /** Persiste un evento de auditoria. Idempotente por eventId. */
    @Transactional
    public void record(EventEnvelope envelope) {
        if (envelope.getPayload() == null) {
            log.warn("Evento de auditoria sin payload, descartado: eventId={}", envelope.getEventId());
            return;
        }
        if (repo.existsByEventId(envelope.getEventId())) {
            log.debug("Auditoria duplicada (eventId ya existe): {}", envelope.getEventId());
            return;
        }

        AuditPayload p;
        try {
            p = mapper.treeToValue(envelope.getPayload(), AuditPayload.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Payload de auditoria no parseable: " + ex.getMessage(), ex);
        }

        AuditLog row = AuditLog.builder()
                .id(UUID.randomUUID())
                .eventId(envelope.getEventId())
                .action(p.getAction())
                .aggregateType(p.getAggregateType() != null ? p.getAggregateType() : envelope.getAggregateType())
                .aggregateId(p.getAggregateId() != null ? p.getAggregateId() : envelope.getAggregateId())
                .businessId(p.getBusinessId() != null ? p.getBusinessId() : envelope.getBusinessId())
                .actorId(p.getActorId())
                .actorName(p.getActorName())
                .occurredAt(p.getOccurredAt() != null ? p.getOccurredAt() : envelope.getOccurredAt())
                .beforeJson(writeJson(p.getBefore()))
                .afterJson(writeJson(p.getAfter()))
                .changedFields(writeJson(p.getChangedFields()))
                .createdAt(Instant.now())
                .build();

        repo.save(row);
        log.info("Auditoria registrada: action={} type={} aggregateId={} actor={}",
                row.getAction(), row.getAggregateType(), row.getAggregateId(), row.getActorName());
    }

    /** Busqueda paginada con filtros dinamicos. */
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> search(UUID businessId,
                                                  String aggregateType,
                                                  UUID aggregateId,
                                                  UUID actorId,
                                                  AuditAction action,
                                                  Instant from,
                                                  Instant to,
                                                  int page,
                                                  int size) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (businessId != null)    ps.add(cb.equal(root.get("businessId"), businessId));
            if (aggregateType != null) ps.add(cb.equal(root.get("aggregateType"), aggregateType));
            if (aggregateId != null)   ps.add(cb.equal(root.get("aggregateId"), aggregateId));
            if (actorId != null)       ps.add(cb.equal(root.get("actorId"), actorId));
            if (action != null)        ps.add(cb.equal(root.get("action"), action));
            if (from != null)          ps.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null)            ps.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<AuditLog> result = repo.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));

        List<AuditLogResponse> content = result.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.of(content, page, size, result.getTotalElements());
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .action(a.getAction())
                .aggregateType(a.getAggregateType())
                .aggregateId(a.getAggregateId())
                .businessId(a.getBusinessId())
                .actorId(a.getActorId())
                .actorName(a.getActorName())
                .occurredAt(a.getOccurredAt())
                .before(readJson(a.getBeforeJson()))
                .after(readJson(a.getAfterJson()))
                .changedFields(readList(a.getChangedFields()))
                .build();
    }

    private String writeJson(Object o) {
        if (o == null) return null;
        try { return mapper.writeValueAsString(o); }
        catch (Exception ex) { return null; }
    }

    private JsonNode readJson(String s) {
        if (s == null) return null;
        try { return mapper.readTree(s); }
        catch (Exception ex) { return null; }
    }

    private List<String> readList(String s) {
        if (s == null) return null;
        try { return mapper.readValue(s, new TypeReference<List<String>>() {}); }
        catch (Exception ex) { return null; }
    }
}
