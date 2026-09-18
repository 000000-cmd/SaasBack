package com.saas.business.domain.availability;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * El turno de un empleado en un dia de la semana, con VIGENCIA.
 *
 * <p>La vigencia es lo que permite cambiarle el horario a alguien sin
 * reescribir su historia: el turno viejo se cierra con {@code validTo} y el
 * nuevo empieza al dia siguiente. Sin ella, cambiar el horario de los martes
 * haria que las citas de los martes pasados dejaran de cuadrar con el turno
 * que en su momento tuvieron.</p>
 *
 * @param validFrom desde cuando aplica (inclusive).
 * @param validTo   hasta cuando (inclusive). {@code null} = sigue vigente.
 */
public record EmployeeShift(
        UUID employeeId,
        DayOfWeek day,
        LocalTime start,
        LocalTime end,
        LocalDate validFrom,
        LocalDate validTo
) {
    /** El turno aplica en esta fecha. */
    public boolean appliesOn(LocalDate date) {
        if (validFrom != null && date.isBefore(validFrom)) return false;
        return validTo == null || !date.isAfter(validTo);
    }

    public boolean crossesMidnight() {
        return !end.isAfter(start);
    }
}
