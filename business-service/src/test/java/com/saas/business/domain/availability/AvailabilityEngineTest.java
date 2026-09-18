package com.saas.business.domain.availability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El motor de disponibilidad, caso por caso.
 *
 * <p>Sin base de datos, sin Spring y sin reloj del sistema: cada prueba
 * construye el escenario a mano y comprueba las horas exactas que salen. Es lo
 * que permite escribir "horario partido" o "servicio que no cabe" como una
 * tabla en vez de como un montaje.</p>
 *
 * <p>Todo ocurre el lunes 2 de marzo de 2026 en Bogota, que no tiene horario de
 * verano. La zona sin cambio de hora es deliberada: mezclar el borde de la
 * medianoche con un salto de reloj haria imposible saber cual de los dos fallo.</p>
 */
class AvailabilityEngineTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate LUNES = LocalDate.of(2026, 3, 2);
    private static final LocalDate MARTES = LUNES.plusDays(1);
    private static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BETO = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    /** El domingo a medianoche: lo bastante antes para que la antelacion no estorbe. */
    private static final Instant AYER = instante(LUNES.minusDays(1), 0, 0);

    // -----------------------------------------------------------------
    // Ayudas
    // -----------------------------------------------------------------

    private static Instant instante(LocalDate d, int h, int m) {
        return LocalDateTime.of(d, LocalTime.of(h, m)).atZone(BOGOTA).toInstant();
    }

    private static DayHours sede(int hIni, int mIni, int hFin, int mFin) {
        return new DayHours(DayOfWeek.MONDAY, LocalTime.of(hIni, mIni), LocalTime.of(hFin, mFin));
    }

    private static EmployeeShift turno(UUID quien, int hIni, int hFin) {
        return new EmployeeShift(quien, DayOfWeek.MONDAY,
                LocalTime.of(hIni, 0), LocalTime.of(hFin, 0), null, null);
    }

    /** Las horas locales de los huecos, como "08:00", para poder afirmarlas de un vistazo. */
    private static List<String> horas(List<Slot> slots) {
        return slots.stream()
                .map(s -> s.startUtc().atZone(BOGOTA).toLocalTime().toString())
                .toList();
    }

    private static AvailabilityInput escenario(List<DayHours> sede, List<EmployeeShift> turnos,
                                               Set<UUID> elegibles, List<BusyBlock> ocupado,
                                               int minutos, BookingPolicy politica,
                                               LocalDate desde, LocalDate hasta, Instant ahora) {
        return new AvailabilityInput(desde, hasta, BOGOTA, sede, turnos, elegibles,
                ocupado, minutos, politica, ahora);
    }

    /** Politica sin restricciones temporales: granularidad y nada mas. */
    private static BookingPolicy simple(int granularidad) {
        return new BookingPolicy(granularidad, 0, 0, 0, 60);
    }

    // -----------------------------------------------------------------
    // Casos
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Horario partido: no ofrece huecos durante el cierre del mediodia")
    void horarioPartido() {
        List<Slot> slots = AvailabilityEngine.compute(escenario(
                List.of(sede(8, 0, 12, 0), sede(14, 0, 18, 0)),
                List.of(turno(ANA, 6, 22)),
                Set.of(ANA), List.of(), 30, simple(60),
                LUNES, LUNES, AYER));

        assertEquals(
                List.of("08:00", "09:00", "10:00", "11:00", "14:00", "15:00", "16:00", "17:00"),
                horas(slots),
                "de 12 a 14 el local esta cerrado y no puede ofrecer nada");
    }

    @Test
    @DisplayName("El turno del empleado manda cuando es menor que el horario del local")
    void turnoMasCortoQueElLocal() {
        List<Slot> slots = AvailabilityEngine.compute(escenario(
                List.of(sede(8, 0, 18, 0)),
                List.of(turno(ANA, 10, 14)),
                Set.of(ANA), List.of(), 60, simple(60),
                LUNES, LUNES, AYER));

        assertEquals(List.of("10:00", "11:00", "12:00", "13:00"), horas(slots),
                "el local abre a las 8, pero Ana no llega hasta las 10");
    }

    @Test
    @DisplayName("Una excepcion parcial abre un agujero, no cierra el dia")
    void excepcionParcial() {
        List<Slot> slots = AvailabilityEngine.compute(escenario(
                List.of(sede(8, 0, 12, 0)),
                List.of(turno(ANA, 8, 12)),
                Set.of(ANA),
                List.of(BusyBlock.exception(ANA, instante(LUNES, 10, 0), instante(LUNES, 11, 0))),
                60, simple(60), LUNES, LUNES, AYER));

        assertEquals(List.of("08:00", "09:00", "11:00"), horas(slots),
                "de 10 a 11 no esta; el resto del dia sigue disponible");
    }

    @Test
    @DisplayName("Un servicio que no cabe en lo que queda no se ofrece")
    void servicioMasLargoQueElHueco() {
        List<Slot> slots = AvailabilityEngine.compute(escenario(
                List.of(sede(8, 0, 9, 0)),
                List.of(turno(ANA, 8, 9)),
                Set.of(ANA), List.of(), 90, simple(30),
                LUNES, LUNES, AYER));

        assertTrue(slots.isEmpty(),
                "hay una hora libre y el servicio dura hora y media: no hay hueco, no medio hueco");
    }

    @Test
    @DisplayName("El buffer de limpieza empuja el siguiente hueco, pero no retrasa la apertura")
    void buffers() {
        List<Slot> slots = AvailabilityEngine.compute(escenario(
                List.of(sede(8, 0, 12, 0)),
                List.of(turno(ANA, 8, 12)),
                Set.of(ANA),
                List.of(BusyBlock.appointment(ANA, instante(LUNES, 9, 0), instante(LUNES, 9, 30))),
                30, new BookingPolicy(30, 0, 15, 0, 60),
                LUNES, LUNES, AYER));

        assertEquals(List.of("08:00", "08:30", "10:00", "10:30", "11:00", "11:30"), horas(slots),
                "la cita acaba a las 9:30 y con 15 min de limpieza libera a las 9:45; "
                        + "el siguiente hueco de la rejilla son las 10:00");
        assertTrue(horas(slots).contains("08:00"),
                "la primera cita del dia puede empezar al abrir: no hay nada anterior que limpiar");
    }

    @Test
    @DisplayName("Un turno de noche cruza la medianoche sin partirse")
    void bordeDeMedianoche() {
        List<DayHours> nocturno = List.of(sede(20, 0, 2, 0));
        List<EmployeeShift> turnoNoche = List.of(new EmployeeShift(
                ANA, DayOfWeek.MONDAY, LocalTime.of(20, 0), LocalTime.of(2, 0), null, null));

        List<Slot> soloLunes = AvailabilityEngine.compute(escenario(
                nocturno, turnoNoche, Set.of(ANA), List.of(), 60, simple(60),
                LUNES, LUNES, AYER));
        assertEquals(List.of("20:00", "21:00", "22:00", "23:00"), horas(soloLunes),
                "pidiendo solo el lunes salen las horas del lunes");

        List<Slot> lunesYMartes = AvailabilityEngine.compute(escenario(
                nocturno, turnoNoche, Set.of(ANA), List.of(), 60, simple(60),
                LUNES, MARTES, AYER));
        assertEquals(List.of("20:00", "21:00", "22:00", "23:00", "00:00", "01:00"),
                horas(lunesYMartes),
                "la madrugada del martes pertenece al turno del lunes y aparece al pedir los dos dias");
    }

    @Test
    @DisplayName("La granularidad cambia cuantos huecos hay, no cuando abre")
    void granularidad() {
        List<DayHours> sede = List.of(sede(8, 0, 10, 0));
        List<EmployeeShift> t = List.of(turno(ANA, 8, 10));

        assertEquals(List.of("08:00", "09:00"),
                horas(AvailabilityEngine.compute(escenario(
                        sede, t, Set.of(ANA), List.of(), 30, simple(60), LUNES, LUNES, AYER))));

        assertEquals(List.of("08:00", "08:30", "09:00", "09:30"),
                horas(AvailabilityEngine.compute(escenario(
                        sede, t, Set.of(ANA), List.of(), 30, simple(30), LUNES, LUNES, AYER))));
    }

    @Test
    @DisplayName("Sin nadie que preste el servicio no hay huecos, y no es un error")
    void nadieHabilitado() {
        List<Slot> slots = AvailabilityEngine.compute(escenario(
                List.of(sede(8, 0, 18, 0)),
                List.of(turno(ANA, 8, 18)),
                Set.of(),               // nadie presta ESTE servicio
                List.of(), 30, simple(30), LUNES, LUNES, AYER));

        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("Un festivo cierra a todo el mundo, no solo a quien lo pidio")
    void festivoCierraLaSede() {
        List<Slot> slots = AvailabilityEngine.compute(escenario(
                List.of(sede(8, 0, 18, 0)),
                List.of(turno(ANA, 8, 18), turno(BETO, 8, 18)),
                Set.of(ANA, BETO),
                // employeeId null = afecta a la sede entera.
                List.of(BusyBlock.exception(null, instante(LUNES, 0, 0), instante(MARTES, 0, 0))),
                30, simple(30), LUNES, LUNES, AYER));

        assertTrue(slots.isEmpty(), "el local esta cerrado: no vale que Beto si tenga turno");
    }

    @Test
    @DisplayName("La antelacion minima descarta lo que ya no da tiempo a reservar")
    void antelacionMinima() {
        Instant lunesA8 = instante(LUNES, 8, 0);

        List<Slot> conUnaHora = AvailabilityEngine.compute(escenario(
                List.of(sede(8, 0, 12, 0)),
                List.of(turno(ANA, 8, 12)),
                Set.of(ANA), List.of(), 30,
                new BookingPolicy(60, 0, 0, 60, 60),   // 60 min de antelacion
                LUNES, LUNES, lunesA8));

        assertEquals(List.of("09:00", "10:00", "11:00"), horas(conUnaHora),
                "son las 8:00 y se pide una hora de antelacion: las 8:00 ya no se puede");
        assertFalse(horas(conUnaHora).contains("08:00"));
    }

    @Test
    @DisplayName("El horizonte maximo corta lo que esta demasiado lejos")
    void horizonteMaximo() {
        List<DayHours> todaLaSemana = List.of(
                new DayHours(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0)),
                new DayHours(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(9, 0)),
                new DayHours(DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(9, 0)));
        List<EmployeeShift> turnos = List.of(
                new EmployeeShift(ANA, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), null, null),
                new EmployeeShift(ANA, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), null, null),
                new EmployeeShift(ANA, DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), null, null));

        List<Slot> slots = AvailabilityEngine.compute(escenario(
                todaLaSemana, turnos, Set.of(ANA), List.of(), 60,
                new BookingPolicy(60, 0, 0, 0, 1),     // horizonte de UN dia
                LUNES, LUNES.plusDays(2), instante(LUNES, 0, 0)));

        assertEquals(1, slots.size(), "con un dia de horizonte solo cabe el lunes");
        assertEquals(LUNES, slots.get(0).startUtc().atZone(BOGOTA).toLocalDate());
    }

    @Test
    @DisplayName("Un turno vencido no cuenta, y el que lo sustituye si")
    void vigenciaDelTurno() {
        // Ana trabajaba de 8 a 12 hasta el domingo, y desde el lunes de 14 a 18.
        List<EmployeeShift> turnos = List.of(
                new EmployeeShift(ANA, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0),
                        null, LUNES.minusDays(1)),
                new EmployeeShift(ANA, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0),
                        LUNES, null));

        List<Slot> slots = AvailabilityEngine.compute(escenario(
                List.of(sede(8, 0, 18, 0)), turnos, Set.of(ANA), List.of(), 60, simple(60),
                LUNES, LUNES, AYER));

        assertEquals(List.of("14:00", "15:00", "16:00", "17:00"), horas(slots),
                "el turno viejo vencio el domingo: el lunes ya no abre por la mañana");
    }

    @Test
    @DisplayName("Dos empleados, dos agendas: lo de uno no tapa lo del otro")
    void agendasIndependientes() {
        List<Slot> slots = AvailabilityEngine.compute(escenario(
                List.of(sede(8, 0, 10, 0)),
                List.of(turno(ANA, 8, 10), turno(BETO, 8, 10)),
                Set.of(ANA, BETO),
                // Ana esta ocupada de 8 a 9; Beto no.
                List.of(BusyBlock.appointment(ANA, instante(LUNES, 8, 0), instante(LUNES, 9, 0))),
                60, simple(60), LUNES, LUNES, AYER));

        assertEquals(3, slots.size());
        assertEquals(1, slots.stream().filter(s -> s.employeeId().equals(ANA)).count(),
                "a Ana solo le queda la hora de las 9");
        assertEquals(2, slots.stream().filter(s -> s.employeeId().equals(BETO)).count(),
                "Beto tiene las dos horas libres");
    }

    @Test
    @DisplayName("Los huecos salen en la rejilla, no arrastrando el desfase de la cita anterior")
    void rejillaLimpia() {
        List<Slot> slots = AvailabilityEngine.compute(escenario(
                List.of(sede(8, 0, 12, 0)),
                List.of(turno(ANA, 8, 12)),
                Set.of(ANA),
                // Una cita rara que acaba a las 9:07.
                List.of(BusyBlock.appointment(ANA, instante(LUNES, 8, 0), instante(LUNES, 9, 7))),
                30, simple(30), LUNES, LUNES, AYER));

        assertEquals(List.of("09:30", "10:00", "10:30", "11:00", "11:30"), horas(slots),
                "termina a las 9:07 y el siguiente hueco es 9:30, no 9:07");
    }
}
