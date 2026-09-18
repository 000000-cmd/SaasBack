package com.saas.business.domain.availability;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Una franja de un dia de la semana, en hora LOCAL del negocio.
 *
 * <p>Hay varias por dia a proposito: un local que abre de 8 a 12 y de 14 a 18
 * son dos franjas, no una con un agujero. Modelarlo con una sola hora de
 * apertura y una de cierre obliga a inventar el cierre del mediodia como una
 * "excepcion", que es exactamente lo que no es.</p>
 *
 * <p>Si {@code end} es anterior o igual a {@code start}, la franja CRUZA LA
 * MEDIANOCHE: un bar que abre de 20:00 a 02:00. El motor lo entiende y no lo
 * descarta como un dato invalido.</p>
 */
public record DayHours(DayOfWeek day, LocalTime start, LocalTime end) {

    public DayHours {
        if (day == null || start == null || end == null) {
            throw new IllegalArgumentException("La franja necesita dia, inicio y fin");
        }
    }

    /** La franja termina al dia siguiente. */
    public boolean crossesMidnight() {
        return !end.isAfter(start);
    }
}
