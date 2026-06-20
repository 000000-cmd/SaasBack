package com.saas.common.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload del evento dedicado de auditoria ({@code audit.recorded}).
 *
 * Es auto-contenido: lleva el actor (resuelto del JWT) y el estado before/after
 * para que el audit-service no dependa de otros servicios. El relay lo enruta
 * al topic {@code audit.events}.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditPayload {

    /** CREATE, UPDATE, DELETE, TOGGLE. */
    private AuditAction action;

    /** "role", "user", "country", ... (mismo vocabulario que aggregateType). */
    private String aggregateType;

    /** Id de la entidad afectada. */
    private UUID aggregateId;

    /** Negocio dueno del cambio. Null = auditoria a nivel sistema. */
    private UUID businessId;

    /** Quien hizo el cambio (del JWT). Null si fue un proceso del sistema. */
    private UUID actorId;
    private String actorName;

    /** Cuando ocurrio el cambio en el dominio. */
    private Instant occurredAt;

    /** Estado anterior (null en CREATE). */
    private JsonNode before;

    /** Estado posterior (null en DELETE). */
    private JsonNode after;

    /** Campos de primer nivel que cambiaron entre before y after (solo UPDATE/TOGGLE). */
    private List<String> changedFields;
}
