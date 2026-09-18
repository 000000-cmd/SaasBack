package com.saas.business.application.dto.request;

import com.saas.business.domain.model.AgendaExceptionKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Bloquear tiempo de agenda.
 *
 * <p>{@code branchId} y {@code employeeId} son excluyentes: los dos nulos
 * cierran el negocio entero, solo sede cierra esa sede, solo empleado es una
 * ausencia suya.</p>
 *
 * @param enabled {@code false} crea el bloqueo desactivado. Es como se prepara
 *        un cierre que todavia tiene citas por reubicar.
 */
public record AgendaExceptionRequest(
        @NotNull UUID businessId,
        UUID branchId,
        UUID employeeId,
        @NotNull Instant startUtc,
        @NotNull Instant endUtc,
        AgendaExceptionKind kind,
        @Size(max = 200) String reason,
        Boolean enabled
) {}
