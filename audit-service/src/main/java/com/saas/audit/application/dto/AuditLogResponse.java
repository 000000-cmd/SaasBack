package com.saas.audit.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.saas.common.audit.AuditAction;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogResponse {
    private UUID id;
    private AuditAction action;
    private String aggregateType;
    private UUID aggregateId;
    private UUID businessId;
    private UUID actorId;
    private String actorName;
    private Instant occurredAt;
    private JsonNode before;
    private JsonNode after;
    private List<String> changedFields;
}
