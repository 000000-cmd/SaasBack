package com.saas.business.domain.availability;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * El calculo de huecos libres.
 *
 * <h3>Es puro</h3>
 * No consulta la base de datos, no lee el reloj del sistema y no sabe que
 * existe Spring. Recibe {@link AvailabilityInput} con todo cargado y devuelve
 * huecos. Por eso se puede probar con casos de tabla —horario partido, turno
 * mas corto que el del local, servicio que no cabe— sin levantar nada, y por
 * eso {@code now} se inyecta: sin ello, "no reservar con menos de 30 minutos"
 * no se puede comprobar.
 *
 * <h3>El orden importa</h3>
 * <ol>
 *   <li>Horario de la sede, dia a dia.</li>
 *   <li>Interseccion con el turno vigente del empleado.</li>
 *   <li>Resta de excepciones (de la sede y suyas).</li>
 *   <li>Resta de citas vivas, con sus buffers.</li>
 *   <li>Recorrido en pasos de granularidad, comprobando que el servicio quepa
 *       ENTERO en lo que queda libre.</li>
 *   <li>Descarte por antelacion minima y por horizonte maximo.</li>
 * </ol>
 *
 * <h3>Los buffers extienden lo ocupado, no lo pedido</h3>
 * Un servicio de 10:00 a 10:30 con 10 minutos de limpieza ocupa hasta las
 * 10:40. Es lo mismo que exigir que cada hueco reserve limpieza por delante,
 * salvo en un caso donde no da igual: la PRIMERA cita del dia. Con esta forma
 * puede empezar a la hora de apertura, porque no hay nada anterior que limpiar.
 * Con la otra, el local abriria diez minutos mas tarde todos los dias sin que
 * nadie entienda por que.
 *
 * <h3>Lo que devuelve es informativo</h3>
 * Que un hueco salga aqui NO lo reserva. El bloqueo ocurre al confirmar, y para
 * entonces puede haberlo cogido otro. Cualquier pantalla que asuma lo contrario
 * acabara ensenando "reservado" sobre algo que ya no existe.
 */
public final class AvailabilityEngine {

    private AvailabilityEngine() { }

    public static List<Slot> compute(AvailabilityInput in) {
        if (in.eligible().isEmpty() || in.branchHours().isEmpty()) {
            // Nadie presta el servicio, o la sede no tiene horario. No es un
            // error: es que no hay nada que ofrecer, y la pantalla debe decirlo
            // en vez de ensenar un calendario vacio sin explicacion.
            return List.of();
        }

        Duration servicio = Duration.ofMinutes(in.serviceMinutes());
        Instant desde = in.now().plus(Duration.ofMinutes(in.policy().minLeadTimeMinutes()));
        Instant hasta = in.now().plus(Duration.ofDays(in.policy().maxHorizonDays()));

        List<Slot> out = new ArrayList<>();
        for (UUID empleado : in.eligible()) {
            List<TimeRange> jornada = TimeRange.intersect(
                    ventanasDeSede(in),
                    ventanasDeTurno(in, empleado));
            if (jornada.isEmpty()) continue;

            List<TimeRange> libres = TimeRange.subtract(jornada, ocupadoPara(in, empleado));

            for (TimeRange hueco : libres) {
                Instant cursor = alinear(hueco.start(), in.zone(), in.policy().slotGranularityMinutes());
                while (!cursor.plus(servicio).isAfter(hueco.end())) {
                    if (!cursor.isBefore(desde) && !cursor.isAfter(hasta)
                            && dentroDelRango(cursor, in)) {
                        out.add(new Slot(cursor, cursor.plus(servicio), empleado));
                    }
                    cursor = cursor.plus(Duration.ofMinutes(in.policy().slotGranularityMinutes()));
                }
            }
        }

        out.sort(Comparator.comparing(Slot::startUtc)
                .thenComparing(s -> s.employeeId().toString()));
        return out;
    }

    // -----------------------------------------------------------------
    // Expansion de horarios a instantes
    // -----------------------------------------------------------------

    /**
     * Las franjas de la sede, dia a dia, ya en UTC.
     *
     * <p>Se empieza un dia ANTES del rango pedido: una franja que cruza la
     * medianoche (un bar de 20:00 a 02:00) pone parte de su horario en el dia
     * siguiente, y si el rango empieza justo ahi habria que descartarla sin
     * motivo. Los huecos que caigan fuera del rango se filtran despues.</p>
     */
    private static List<TimeRange> ventanasDeSede(AvailabilityInput in) {
        List<TimeRange> out = new ArrayList<>();
        for (LocalDate d = in.from().minusDays(1); !d.isAfter(in.to()); d = d.plusDays(1)) {
            for (DayHours h : in.branchHours()) {
                if (h.day() != d.getDayOfWeek()) continue;
                out.add(aRango(d, h.start(), h.end(), h.crossesMidnight(), in.zone()));
            }
        }
        return out;
    }

    private static List<TimeRange> ventanasDeTurno(AvailabilityInput in, UUID empleado) {
        List<TimeRange> out = new ArrayList<>();
        for (LocalDate d = in.from().minusDays(1); !d.isAfter(in.to()); d = d.plusDays(1)) {
            for (EmployeeShift s : in.shifts()) {
                if (!s.employeeId().equals(empleado)) continue;
                if (s.day() != d.getDayOfWeek()) continue;
                // La vigencia se mira contra el dia que se esta expandiendo:
                // un turno que cambio el 15 no puede aplicarse al dia 10.
                if (!s.appliesOn(d)) continue;
                out.add(aRango(d, s.start(), s.end(), s.crossesMidnight(), in.zone()));
            }
        }
        return out;
    }

    /**
     * Lo ocupado para este empleado, con los buffers ya sumados a las citas.
     *
     * <p>Un bloque sin empleado (un festivo, el local cerrado) afecta a todos y
     * entra aqui igual.</p>
     */
    private static List<TimeRange> ocupadoPara(AvailabilityInput in, UUID empleado) {
        int antes = in.policy().bufferBeforeMinutes();
        int despues = in.policy().bufferAfterMinutes();

        List<TimeRange> out = new ArrayList<>();
        for (BusyBlock b : in.busy()) {
            if (!b.affects(empleado)) continue;
            Instant ini = b.bufferable() ? b.startUtc().minus(Duration.ofMinutes(antes)) : b.startUtc();
            Instant fin = b.bufferable() ? b.endUtc().plus(Duration.ofMinutes(despues)) : b.endUtc();
            out.add(new TimeRange(ini, fin));
        }
        return out;
    }

    private static TimeRange aRango(LocalDate dia, LocalTime ini, LocalTime fin,
                                    boolean cruzaMedianoche, ZoneId zona) {
        ZonedDateTime desde = LocalDateTime.of(dia, ini).atZone(zona);
        LocalDate diaFin = cruzaMedianoche ? dia.plusDays(1) : dia;
        ZonedDateTime hasta = LocalDateTime.of(diaFin, fin).atZone(zona);
        return new TimeRange(desde.toInstant(), hasta.toInstant());
    }

    /**
     * Sube el instante al siguiente multiplo de la granularidad, contando desde
     * la MEDIANOCHE LOCAL.
     *
     * <p>Contar desde medianoche y no desde el principio del hueco es lo que
     * hace que las horas salgan redondas —:00 :15 :30 :45— en vez de heredar el
     * desfase de la cita anterior. Un local que abre a las 8:00 y tiene una cita
     * que termina a las 9:07 ofrece las 9:15, no las 9:07.</p>
     */
    private static Instant alinear(Instant t, ZoneId zona, int granularidad) {
        ZonedDateTime local = t.atZone(zona);
        int minutosDelDia = local.getHour() * 60 + local.getMinute();
        int resto = minutosDelDia % granularidad;

        boolean yaEstaEnLaRejilla = resto == 0 && local.getSecond() == 0 && local.getNano() == 0;
        if (yaEstaEnLaRejilla) return t;

        int subir = granularidad - resto;
        return local.withSecond(0).withNano(0).plusMinutes(subir).toInstant();
    }

    /** El hueco cae en un dia del rango pedido, en hora local. */
    private static boolean dentroDelRango(Instant t, AvailabilityInput in) {
        LocalDate d = t.atZone(in.zone()).toLocalDate();
        return !d.isBefore(in.from()) && !d.isAfter(in.to());
    }
}
