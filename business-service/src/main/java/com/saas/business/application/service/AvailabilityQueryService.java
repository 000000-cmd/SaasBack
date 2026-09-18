package com.saas.business.application.service;

import com.saas.business.domain.availability.AvailabilityEngine;
import com.saas.business.domain.availability.AvailabilityInput;
import com.saas.business.domain.availability.BookingPolicy;
import com.saas.business.domain.availability.BusyBlock;
import com.saas.business.domain.availability.DayHours;
import com.saas.business.domain.availability.EmployeeShift;
import com.saas.business.domain.availability.Slot;
import com.saas.business.domain.model.AgendaException;
import com.saas.business.domain.model.Appointment;
import com.saas.business.domain.model.BranchSchedule;
import com.saas.business.domain.model.BranchScheduleShift;
import com.saas.business.domain.model.Business;
import com.saas.business.domain.model.Employee;
import com.saas.business.domain.model.EmployeeOffering;
import com.saas.business.domain.model.EmployeeShiftAssignment;
import com.saas.business.domain.model.Offering;
import com.saas.business.domain.port.in.IBusinessBookingPolicyUseCase;
import com.saas.business.domain.port.in.IBusinessUseCase;
import com.saas.business.domain.port.in.IEmployeeUseCase;
import com.saas.business.domain.port.in.IOfferingUseCase;
import com.saas.business.domain.port.out.IAgendaExceptionRepositoryPort;
import com.saas.business.domain.port.out.IAppointmentRepositoryPort;
import com.saas.business.domain.port.out.IBranchScheduleRepositoryPort;
import com.saas.business.domain.port.out.IBranchScheduleShiftRepositoryPort;
import com.saas.business.domain.port.out.IEmployeeOfferingRepositoryPort;
import com.saas.business.domain.port.out.IEmployeeShiftAssignmentRepositoryPort;
import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Le da de comer al motor de disponibilidad.
 *
 * <p>El motor es puro: no consulta nada. Todo lo que necesita —horarios de la
 * sede, turnos vigentes, quien presta el servicio, citas y bloqueos— se carga
 * AQUI y se le entrega ya resuelto. Esa separacion es lo que permite probar el
 * calculo con casos de tabla sin levantar base de datos, y es tambien lo que
 * evita que el calculo dispare consultas dentro de sus propios bucles.</p>
 *
 * <h3>Consultas por calculo</h3>
 * Siete, todas por lote y ninguna dentro de un bucle: negocio, politica,
 * catalogo de servicios, empleados de la sede, quien presta esos servicios,
 * horarios + turnos, y lo ocupado (citas y excepciones). El catalogo de dias de
 * la semana va en cache: son siete filas que no cambian.
 *
 * <h3>La duracion puede variar por empleado</h3>
 * {@code employee_offering.DurationMinutes} permite que el aprendiz tarde 45
 * minutos en el mismo corte que el maestro hace en 30. Como el motor calcula
 * para una duracion dada, los empleados se agrupan por duracion total y se
 * lanza una pasada por grupo. En la practica es una sola: casi nadie
 * sobreescribe.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityQueryService {

    /** Tope de dias por consulta. Un calendario pide un mes; nadie pide un ano. */
    private static final int MAX_DIAS = 62;

    private final IBusinessUseCase businesses;
    private final IOfferingUseCase offerings;
    private final IEmployeeUseCase employees;
    private final IBusinessBookingPolicyUseCase policies;
    private final IEmployeeOfferingRepositoryPort employeeOfferings;
    private final IBranchScheduleRepositoryPort branchSchedules;
    private final IBranchScheduleShiftRepositoryPort branchShifts;
    private final IEmployeeShiftAssignmentRepositoryPort assignments;
    private final IAppointmentRepositoryPort appointments;
    private final IAgendaExceptionRepositoryPort exceptions;
    private final DayOfWeekCatalog diasDeLaSemana;

    /**
     * Lo que se pide.
     *
     * @param employeeId opcional: filtra a un profesional concreto. Es el caso
     *        "quiero con Ana", que no es lo mismo que "el primero que pueda".
     */
    public record Query(UUID businessId, UUID branchId, List<UUID> offeringIds,
                        UUID employeeId, LocalDate from, LocalDate to) {}

    /**
     * Los huecos y la zona con la que se calcularon.
     *
     * <p>La zona viaja con el resultado porque quien pinta el calendario tiene
     * que agrupar por el dia LOCAL DEL NEGOCIO. Si el front agrupara con la
     * zona del navegador, un cliente de Madrid veria las citas de la manana
     * colombiana repartidas en el dia siguiente.</p>
     */
    public record Result(ZoneId zone, List<Slot> slots) {}

    @Transactional(readOnly = true)
    public Result slots(Query q) {
        return slots(q, Instant.now());
    }

    /** Con {@code now} explicito: asi la antelacion minima se puede probar. */
    @Transactional(readOnly = true)
    public Result slots(Query q, Instant now) {
        validar(q);

        Business negocio = businesses.getById(q.businessId());
        ZoneId zona = zonaDe(negocio);
        BookingPolicy politica = policies.forBusiness(q.businessId()).toEnginePolicy();

        Map<UUID, Offering> catalogo = new HashMap<>();
        for (Offering o : offerings.findByBusiness(q.businessId())) catalogo.put(o.getId(), o);

        int duracionBase = 0;
        for (UUID id : q.offeringIds()) {
            Offering o = catalogo.get(id);
            if (o == null) throw new ResourceNotFoundException("Servicio", "Id", id);
            if (!Boolean.TRUE.equals(o.getIsActive())) {
                throw new BusinessException("El servicio \"" + o.getName() + "\" ya no está disponible");
            }
            if (o.getDurationMinutes() == null || o.getDurationMinutes() <= 0) {
                throw new BusinessException(
                        "El servicio \"" + o.getName() + "\" no tiene duración configurada");
            }
            duracionBase += o.getDurationMinutes();
        }

        // --- Quien puede atender ---------------------------------------
        Map<UUID, Employee> candidatos = new LinkedHashMap<>();
        for (Employee e : employees.findByBranch(q.branchId())) {
            if (q.employeeId() != null && !q.employeeId().equals(e.getId())) continue;
            if (Boolean.FALSE.equals(e.getEnabled())) continue;
            if (e.getTerminationDate() != null && !e.getTerminationDate().isAfter(LocalDate.now(zona))) continue;
            candidatos.put(e.getId(), e);
        }
        if (candidatos.isEmpty()) return new Result(zona, List.of());

        Map<UUID, Integer> duracionPorEmpleado =
                elegibles(q.offeringIds(), candidatos.keySet(), catalogo, duracionBase);
        if (duracionPorEmpleado.isEmpty()) return new Result(zona, List.of());

        // --- Ventana de carga ------------------------------------------
        // Un dia antes y uno despues: una franja que cruza la medianoche pone
        // parte de su horario en el dia de al lado, y si se recorta la ventana
        // justo en el borde se pierde media noche de agenda.
        Instant desde = q.from().minusDays(1).atStartOfDay(zona).toInstant();
        Instant hasta = q.to().plusDays(2).atStartOfDay(zona).toInstant();

        List<BranchScheduleShift> turnosSede = turnosDeLaSede(q.branchId());
        List<DayHours> horarioSede = horarioDeSede(turnosSede);
        if (horarioSede.isEmpty()) {
            log.debug("La sede {} no tiene horario configurado: sin disponibilidad", q.branchId());
            return new Result(zona, List.of());
        }

        List<EmployeeShift> turnos = turnosDe(
                duracionPorEmpleado.keySet(), turnosSede, desde, hasta, zona);
        List<BusyBlock> ocupado = ocupado(q, desde, hasta);

        // --- Una pasada por duracion distinta --------------------------
        Map<Integer, Set<UUID>> porDuracion = new LinkedHashMap<>();
        duracionPorEmpleado.forEach((empleado, minutos) ->
                porDuracion.computeIfAbsent(minutos, k -> new HashSet<>()).add(empleado));

        List<Slot> salida = new ArrayList<>();
        for (Map.Entry<Integer, Set<UUID>> grupo : porDuracion.entrySet()) {
            salida.addAll(AvailabilityEngine.compute(new AvailabilityInput(
                    q.from(), q.to(), zona, horarioSede, turnos, grupo.getValue(),
                    ocupado, grupo.getKey(), politica, now)));
        }
        salida.sort(Comparator.comparing(Slot::startUtc)
                .thenComparing(s -> s.employeeId().toString()));
        return new Result(zona, salida);
    }

    // -----------------------------------------------------------------
    // Carga
    // -----------------------------------------------------------------

    private void validar(Query q) {
        if (q.offeringIds() == null || q.offeringIds().isEmpty()) {
            throw new BusinessException("Hay que decir qué servicio se quiere");
        }
        if (q.from() == null || q.to() == null || q.to().isBefore(q.from())) {
            throw new BusinessException("Rango de fechas inválido");
        }
        if (q.from().plusDays(MAX_DIAS).isBefore(q.to())) {
            throw new BusinessException("Consulta como máximo " + MAX_DIAS + " días de una vez");
        }
    }

    private ZoneId zonaDe(Business negocio) {
        try {
            return ZoneId.of(negocio.getTimeZone() == null ? "America/Bogota" : negocio.getTimeZone());
        } catch (RuntimeException ex) {
            // Una zona mal escrita en la ficha del negocio no puede tumbar la
            // agenda entera. Se registra y se sigue con la de casa.
            log.warn("Zona horaria inválida en el negocio {}: {}", negocio.getId(), negocio.getTimeZone());
            return ZoneId.of("America/Bogota");
        }
    }

    /**
     * Los empleados que prestan TODOS los servicios pedidos, con su duracion
     * total.
     *
     * <p>Todos y no alguno: una cita de corte + barba con quien solo hace corte
     * no es media cita, es una cita imposible.</p>
     */
    private Map<UUID, Integer> elegibles(List<UUID> pedidos, Set<UUID> candidatos,
                                         Map<UUID, Offering> catalogo, int duracionBase) {
        Map<UUID, Map<UUID, EmployeeOffering>> porEmpleado = new HashMap<>();
        for (EmployeeOffering eo : employeeOfferings.findByOfferingIds(pedidos)) {
            if (!candidatos.contains(eo.getEmployeeId())) continue;
            if (Boolean.FALSE.equals(eo.getEnabled())) continue;
            porEmpleado.computeIfAbsent(eo.getEmployeeId(), k -> new HashMap<>())
                    .put(eo.getOfferingId(), eo);
        }

        Map<UUID, Integer> out = new LinkedHashMap<>();
        porEmpleado.forEach((empleado, suyos) -> {
            if (!suyos.keySet().containsAll(pedidos)) return;
            int minutos = 0;
            for (UUID id : pedidos) {
                Integer propia = suyos.get(id).getDurationMinutes();
                minutos += propia != null && propia > 0
                        ? propia
                        : catalogo.get(id).getDurationMinutes();
            }
            out.put(empleado, minutos > 0 ? minutos : duracionBase);
        });
        return out;
    }

    /** Los turnos de los horarios VIGENTES de la sede. Una sola consulta. */
    private List<BranchScheduleShift> turnosDeLaSede(UUID branchId) {
        List<BranchSchedule> vigentes = branchSchedules.findByBranchIdAndValidToIsNull(branchId);
        if (vigentes.isEmpty()) return List.of();
        return branchShifts.findByBranchScheduleIds(
                vigentes.stream().map(BranchSchedule::getId).toList());
    }

    /** El horario de apertura de la sede. */
    private List<DayHours> horarioDeSede(List<BranchScheduleShift> turnosSede) {
        Map<UUID, DayOfWeek> dias = diasDeLaSemana.isoById();
        List<DayHours> out = new ArrayList<>();
        for (BranchScheduleShift t : turnosSede) {
            if (Boolean.FALSE.equals(t.getEnabled())) continue;
            DayOfWeek dia = dias.get(t.getDayOfWeekId());
            if (dia == null || t.getStartTime() == null || t.getEndTime() == null) continue;
            out.add(new DayHours(dia, t.getStartTime(), t.getEndTime()));
        }
        return out;
    }

    /**
     * Los turnos de los empleados, con la vigencia de su asignacion.
     *
     * <p>Los turnos se buscan en los que YA se cargaron de la sede. Una
     * asignacion que apunte a otro sitio —a un horario cerrado, o al de otra
     * sede— se ignora: ese empleado no atiende aqui hoy.</p>
     */
    private List<EmployeeShift> turnosDe(Set<UUID> empleados, List<BranchScheduleShift> turnosSede,
                                         Instant desde, Instant hasta, ZoneId zona) {
        LocalDateTime ini = LocalDateTime.ofInstant(desde, zona);
        LocalDateTime fin = LocalDateTime.ofInstant(hasta, zona);
        List<EmployeeShiftAssignment> asignaciones = assignments.effectiveIn(empleados, ini, fin);
        if (asignaciones.isEmpty()) return List.of();

        Map<UUID, BranchScheduleShift> turnos = new HashMap<>();
        for (BranchScheduleShift t : turnosSede) turnos.put(t.getId(), t);

        Map<UUID, DayOfWeek> dias = diasDeLaSemana.isoById();
        List<EmployeeShift> out = new ArrayList<>();
        for (EmployeeShiftAssignment a : asignaciones) {
            BranchScheduleShift t = turnos.get(a.getBranchScheduleShiftId());
            if (t == null) continue;
            DayOfWeek dia = dias.get(t.getDayOfWeekId());
            if (dia == null) continue;

            // Turno partido: si la asignacion no es completa manda su horario
            // propio. Es como se modela "entra a la misma hora pero sale antes".
            boolean completo = !Boolean.FALSE.equals(a.getIsFullShift());
            var inicio = completo || a.getCustomStartTime() == null ? t.getStartTime() : a.getCustomStartTime();
            var finTurno = completo || a.getCustomEndTime() == null ? t.getEndTime() : a.getCustomEndTime();
            if (inicio == null || finTurno == null) continue;

            out.add(new EmployeeShift(a.getEmployeeId(), dia, inicio, finTurno,
                    a.getValidFrom() == null ? null : a.getValidFrom().toLocalDate(),
                    a.getValidTo() == null ? null : a.getValidTo().toLocalDate()));
        }
        return out;
    }

    /** Citas vivas y bloqueos, ya como bloques ocupados. */
    private List<BusyBlock> ocupado(Query q, Instant desde, Instant hasta) {
        List<BusyBlock> out = new ArrayList<>();

        for (Appointment a : appointments.busyInRange(q.businessId(), desde, hasta)) {
            if (!q.branchId().equals(a.getBranchId())) continue;
            out.add(BusyBlock.appointment(a.getEmployeeId(), a.getStartUtc(), a.getEndUtc()));
        }

        for (AgendaException e : exceptions.overlapping(q.businessId(), desde, hasta)) {
            // Un bloqueo de OTRA sede no cierra esta. Uno sin sede afecta a todas.
            if (e.getBranchId() != null && !e.getBranchId().equals(q.branchId())) continue;
            out.add(BusyBlock.exception(e.getEmployeeId(), e.getStartUtc(), e.getEndUtc()));
        }
        return out;
    }

}
