package com.saas.business.domain.availability;

import java.time.Instant;
import java.util.UUID;

/**
 * Un trozo de agenda que NO esta libre.
 *
 * <p>Cubre las dos cosas que restan tiempo, y las trata casi igual porque para
 * el calculo lo son: una cita viva y una excepcion (festivo, vacaciones,
 * bloqueo de una tarde).</p>
 *
 * @param employeeId a quien afecta. {@code null} = afecta a TODOS: el local
 *                   cerrado por festivo no deja hueco a nadie.
 * @param bufferable si los buffers de preparacion y limpieza se le suman.
 *                   {@code true} en una cita: despues de un corte hay que
 *                   barrer. {@code false} en una excepcion: un festivo no
 *                   necesita que le limpien alrededor, y sumarle buffers
 *                   comeria media hora de agenda buena a cada lado.
 */
public record BusyBlock(UUID employeeId, Instant startUtc, Instant endUtc, boolean bufferable) {

    /** Una cita: ocupa y ademas arrastra los buffers. */
    public static BusyBlock appointment(UUID employeeId, Instant start, Instant end) {
        return new BusyBlock(employeeId, start, end, true);
    }

    /** Una excepcion: ocupa exactamente lo que dice, ni un minuto mas. */
    public static BusyBlock exception(UUID employeeId, Instant start, Instant end) {
        return new BusyBlock(employeeId, start, end, false);
    }

    public TimeRange range() {
        return new TimeRange(startUtc, endUtc);
    }

    /** Afecta a este empleado, sea por ser suyo o por ser de toda la sede. */
    public boolean affects(UUID candidate) {
        return employeeId == null || employeeId.equals(candidate);
    }
}
