package com.saas.business.infrastructure.controller;

import com.saas.business.domain.model.Appointment;
import com.saas.business.domain.model.AppointmentLine;
import com.saas.business.domain.model.BusinessClient;
import com.saas.business.domain.port.out.IAppointmentRepositoryPort;
import com.saas.business.domain.port.out.IBusinessClientRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que finance necesita saber de la agenda para cobrar.
 *
 * <h3>Por que es una consulta y no un evento</h3>
 * <p>Finance PREGUNTA por las citas de una ventana y crea los cargos que le
 * falten. Si la consulta falla, la siguiente pasada las recoge; si finance
 * estuvo caido un dia, al volver se pone al dia solo. Un evento perdido, en
 * cambio, no vuelve: habria que reprocesar la cola a mano, y lo que se pierde
 * es la comision de alguien.</p>
 *
 * <p>La otra opcion —llamar a finance al completar la cita— mete una llamada de
 * red dentro de la transaccion de la agenda: si finance no responde, o la cita
 * no se completa o el cargo no se crea. Las dos son peores.</p>
 *
 * <h3>Lo que viaja</h3>
 * <p>El SNAPSHOT de la cita: el precio y la duracion que se congelaron al
 * agendar, no los del catalogo de hoy. Si el dueno sube el precio en julio, la
 * cita de marzo sigue valiendo lo de marzo, y de ahi sale la comision.</p>
 */
@Slf4j
@RestController
@RequestMapping("/internal/appointments")
@RequiredArgsConstructor
public class InternalAppointmentController {

    private final IAppointmentRepositoryPort appointments;
    private final IBusinessClientRepositoryPort clients;

    /**
     * Una cita vista desde finance: lo justo para crear su cargo.
     *
     * @param serviceName los servicios de la cita, ya unidos ("Corte + Barba").
     *        Finance guarda el nombre y no el id porque el suyo es un
     *        comprobante: tiene que seguir leyendose aunque el servicio
     *        desaparezca del catalogo.
     */
    public record AppointmentForCharge(
            UUID appointmentId, UUID businessId, UUID branchId, UUID employeeId,
            String status, boolean backdated,
            LocalDate serviceDate, LocalTime startTime, LocalTime endTime,
            String serviceName, BigDecimal totalPrice, Integer totalMinutes,
            String clientName, String clientPhone, UUID clientThirdPartyId) {}

    /**
     * Las citas de un negocio en un rango de fechas locales, con su snapshot.
     *
     * <p>Se devuelven TODAS las que ocupan agenda o ya se prestaron, incluidas
     * las canceladas, y es finance quien decide: necesita distinguir la que
     * todavia no ha pasado (cargo programado) de la ya prestada (cargo
     * pendiente de aprobar) y de la que nunca se prestara (ningun cargo).</p>
     */
    @GetMapping("/for-charges")
    public List<AppointmentForCharge> forCharges(
            @RequestParam UUID businessId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<Appointment> citas = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            citas.addAll(appointments.byBusinessAndDay(businessId, d));
        }
        if (citas.isEmpty()) return List.of();

        // Clientes y lineas de una vez: pedirlos cita a cita serian dos viajes
        // por fila para datos que caben en dos consultas.
        Map<UUID, BusinessClient> porId = new HashMap<>();
        for (BusinessClient c : clients.findByBusinessId(businessId)) porId.put(c.getId(), c);

        Map<UUID, List<AppointmentLine>> lineasPorCita = new HashMap<>();
        for (AppointmentLine l : appointments.linesOfMany(citas.stream().map(Appointment::getId).toList())) {
            lineasPorCita.computeIfAbsent(l.getAppointmentId(), k -> new ArrayList<>()).add(l);
        }

        List<AppointmentForCharge> out = new ArrayList<>(citas.size());
        for (Appointment a : citas) {
            ZoneId zona = zonaDe(a);
            BusinessClient cliente = porId.get(a.getBusinessClientId());
            out.add(new AppointmentForCharge(
                    a.getId(), a.getBusinessId(), a.getBranchId(), a.getEmployeeId(),
                    a.getStatus().name(), a.isBackdated(),
                    a.getLocalDate(), hora(a.getStartUtc(), zona), hora(a.getEndUtc(), zona),
                    nombreServicios(lineasPorCita.get(a.getId())),
                    a.getTotalPrice(), a.getTotalDurationMinutes(),
                    cliente == null ? null : cliente.getDisplayName(),
                    cliente == null ? null : cliente.getPhoneE164(),
                    cliente == null ? null : cliente.getThirdPartyId()));
        }
        return out;
    }

    private static ZoneId zonaDe(Appointment a) {
        try {
            return ZoneId.of(a.getBusinessTimeZone() == null
                    ? "America/Bogota" : a.getBusinessTimeZone());
        } catch (RuntimeException ex) {
            return ZoneId.of("America/Bogota");
        }
    }

    private static LocalTime hora(Instant t, ZoneId zona) {
        return t == null ? null : t.atZone(zona).toLocalTime();
    }

    /** "Corte + Barba". Si la cita no tiene lineas, algo raro pasa: no se calla. */
    private static String nombreServicios(List<AppointmentLine> lineas) {
        if (lineas == null || lineas.isEmpty()) return "Servicio";
        return String.join(" + ", lineas.stream().map(AppointmentLine::getServiceName).toList());
    }
}
