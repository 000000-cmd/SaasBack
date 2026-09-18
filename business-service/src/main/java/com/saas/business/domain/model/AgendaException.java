package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Todo lo que RESTA tiempo del horario normal.
 *
 * <p>Un festivo, unas vacaciones, una incapacidad, o bloquear la tarde de un
 * martes concreto. Con rango de fecha-HORA y no por dias completos: bloquear
 * "de 2 a 4" es el caso mas frecuente y el que un modelo por dias no sabe
 * expresar.</p>
 *
 * <p>{@code branchId} y {@code employeeId} son excluyentes en la practica:</p>
 * <ul>
 *   <li>los dos nulos: afecta a todo el negocio (un festivo);</li>
 *   <li>solo sede: cierra esa sede;</li>
 *   <li>solo empleado: ausencia de esa persona.</li>
 * </ul>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class AgendaException extends BaseDomain {
    private UUID branchId;
    private UUID employeeId;
    private Instant startUtc;
    private Instant endUtc;
    @Builder.Default private AgendaExceptionKind kind = AgendaExceptionKind.BLOCK;
    private String reason;
}
