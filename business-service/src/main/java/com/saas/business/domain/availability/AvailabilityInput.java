package com.saas.business.domain.availability;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Todo lo que el motor necesita, YA CARGADO.
 *
 * <p>El motor no consulta la base de datos por dentro. Quien lo llama trae los
 * horarios, los turnos, las excepciones y las citas, y recibe huecos. Esa es la
 * razon de que se pueda probar con casos de tabla: horario partido, turno mas
 * corto que el del local, servicio que no cabe en lo que queda... todos son
 * construir este record a mano y comprobar la lista que sale.</p>
 *
 * @param from            primer dia del rango (fecha LOCAL del negocio).
 * @param to              ultimo dia, inclusive.
 * @param zone            zona horaria del negocio. Todo lo demas va en UTC.
 * @param branchHours     franjas de apertura de la sede, por dia de la semana.
 * @param shifts          turnos de los empleados, con su vigencia.
 * @param eligible        empleados que SI prestan el servicio pedido. Si esta
 *                        vacio no hay disponibilidad: no es un error, es que
 *                        nadie lo hace.
 * @param busy            lo que ya ocupa agenda: citas vivas y excepciones.
 *                        Llegan con los buffers ya aplicados (ver el motor).
 * @param serviceMinutes  cuanto dura el servicio pedido, o la suma si son varios.
 * @param policy          las reglas del negocio.
 * @param now             el instante actual. Se inyecta y no se lee del reloj
 *                        del sistema: sin esto, "no reservar con menos de 30
 *                        minutos" es imposible de probar.
 */
public record AvailabilityInput(
        LocalDate from,
        LocalDate to,
        ZoneId zone,
        List<DayHours> branchHours,
        List<EmployeeShift> shifts,
        Set<java.util.UUID> eligible,
        List<BusyBlock> busy,
        int serviceMinutes,
        BookingPolicy policy,
        Instant now
) {
    public AvailabilityInput {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Rango de fechas invalido");
        }
        if (serviceMinutes <= 0) {
            throw new IllegalArgumentException("El servicio tiene que durar algo");
        }
        branchHours = branchHours == null ? List.of() : List.copyOf(branchHours);
        shifts = shifts == null ? List.of() : List.copyOf(shifts);
        eligible = eligible == null ? Set.of() : Set.copyOf(eligible);
        busy = busy == null ? List.of() : List.copyOf(busy);
    }
}
