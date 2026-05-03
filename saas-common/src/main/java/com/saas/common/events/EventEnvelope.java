package com.saas.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

//Estandar de todo lo que viaja por kafka
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEnvelope {

    /**
     * Versión del envelope (este wrapper). Si se agregan campos nuevos, sube a 2.
     */
    @Builder.Default
    private int _v = 1;

    /**
     * Único globalmente. Lo usa el consumer para idempotencia (descartar duplicados).
     */
    private UUID eventId;

    /** "user.created", "role.updated", etc. Ver {@link EventTypes}. */
    private String type;

    /**
     * Versión del payload de este type. Sube cuando cambies el shape del payload.
     */
    @Builder.Default
    private int version = 1;

    /**
     * Multi-tenancy. Nullable hasta que tengas la entidad Business.
     */
    private UUID businessId;

    /**
     * Id de la entidad afectada (userId, roleId, payrollRunId).
     */
    private UUID aggregateId;

    /**
     * "user", "role", "payroll-run", "stock-movement".
     */
    private String aggregateType;

    /**
     * Cuándo ocurrió el cambio en el dominio (no cuándo se publicó).
     */
    private Instant occurredAt;

    /**
     * Quién emitió el evento. Útil para debugging y auditoría.
     */
    private String producer;

    /**
     * Datos específicos del evento. Estructura libre — definida por cada {@code type}.
     */
    private JsonNode payload;

}