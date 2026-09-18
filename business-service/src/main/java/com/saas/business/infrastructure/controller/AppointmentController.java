package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.BookAppointmentRequest;
import com.saas.business.application.service.AppointmentBookingService;
import com.saas.business.application.service.BookingCommand;
import com.saas.business.domain.model.Appointment;
import com.saas.business.domain.model.AppointmentChannel;
import com.saas.business.domain.model.AppointmentLine;
import com.saas.business.domain.model.AppointmentStatus;
import com.saas.business.domain.model.Business;
import com.saas.business.domain.model.BusinessBookingPolicy;
import com.saas.business.domain.model.BusinessClient;
import com.saas.business.domain.model.Employee;
import com.saas.business.domain.model.EmployeeOffering;
import com.saas.business.domain.model.Offering;
import com.saas.business.domain.model.PublicCode;
import com.saas.business.domain.port.in.IBusinessBookingPolicyUseCase;
import com.saas.business.domain.port.in.IBusinessUseCase;
import com.saas.business.domain.port.in.IOfferingUseCase;
import com.saas.business.domain.port.out.IAppointmentRepositoryPort;
import com.saas.business.domain.port.out.IBusinessClientRepositoryPort;
import com.saas.business.domain.port.out.IEmployeeOfferingRepositoryPort;
import com.saas.business.domain.port.out.IEmployeeRepositoryPort;
import com.saas.business.infrastructure.client.SystemClient;
import com.saas.business.infrastructure.client.ThirdPartyClient;
import com.saas.common.dto.ApiResponse;
import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.security.IUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * La agenda desde el panel y desde el APK.
 *
 * <p>Los precios y las duraciones NO llegan del cliente: se resuelven del
 * catalogo y se congelan en la cita. Si viajaran en la peticion, podrian
 * viajar a cero.</p>
 *
 * <p>La comision se deja en cero al reservar a proposito. Se congela cuando la
 * cita se completa y finance crea el cargo, que es donde ya vive la
 * compensacion versionada: resolverla aqui obligaria a una llamada entre
 * servicios en cada reserva para un dato que todavia puede cambiar antes de que
 * el servicio se preste.</p>
 */
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentBookingService booking;
    private final IAppointmentRepositoryPort repo;
    private final IOfferingUseCase offerings;
    private final IBusinessUseCase businesses;
    private final IBusinessBookingPolicyUseCase policies;
    private final IEmployeeOfferingRepositoryPort employeeOfferings;
    private final IEmployeeRepositoryPort employees;
    private final IBusinessClientRepositoryPort clients;
    private final SystemClient system;
    private final ThirdPartyClient thirdParties;
    private final com.saas.business.application.service.AgendaSweepJob sweepJob;

    /** Quien puede anotar un servicio ya prestado. Lo exige tambien el boton. */
    private static final String PERMISO_RETROACTIVO = "APPOINTMENT_BACKDATE";

    public record AppointmentView(
            UUID id, String publicCode, UUID businessId, UUID branchId, UUID employeeId,
            UUID businessClientId, String status, String channel,
            Instant startUtc, Instant endUtc, String timeZone, LocalDate localDate,
            boolean backdated, BigDecimal totalPrice, Integer totalMinutes, String notes,
            String serviceName, String clientName, String clientPhone) {}

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentView>> book(
            @AuthenticationPrincipal IUserPrincipal principal,
            @Valid @RequestBody BookAppointmentRequest req) {

        // El registro retroactivo se comprueba por PERMISO, no por rol ni por
        // canal. El boton del flujo ya lo exige; si el endpoint se conformara
        // con "viene del panel", esconder el boton no protegeria nada.
        if (req.backdated() && !system
                .permissionCodesByRoleCodes(principal.getRoles())
                .contains(PERMISO_RETROACTIVO)) {
            throw new AccessDeniedException(
                    "No tienes permiso para registrar un servicio ya prestado");
        }

        Business negocio = businesses.getById(req.businessId());
        ZoneId zona = ZoneId.of(negocio.getTimeZone() == null
                ? "America/Bogota" : negocio.getTimeZone());

        // Del catalogo, no de la peticion.
        Map<UUID, Offering> catalogo = offerings.findByBusiness(req.businessId()).stream()
                .collect(Collectors.toMap(Offering::getId, o -> o));

        // La duracion puede estar sobreescrita para ESTE empleado: el aprendiz
        // tarda mas en el mismo corte. Si aqui se usara la del catalogo, la
        // cita duraria menos de lo que ocupa y la agenda se solaparia sola.
        Map<UUID, Integer> propias = employeeOfferings.findByOfferingIds(req.offeringIds()).stream()
                .filter(eo -> req.employeeId().equals(eo.getEmployeeId()))
                .filter(eo -> eo.getDurationMinutes() != null && eo.getDurationMinutes() > 0)
                .collect(Collectors.toMap(EmployeeOffering::getOfferingId,
                        EmployeeOffering::getDurationMinutes, (a, b) -> a));

        List<BookingCommand.Line> lineas = req.offeringIds().stream()
                .map(id -> {
                    Offering o = catalogo.get(id);
                    if (o == null) {
                        throw new ResourceNotFoundException("Servicio", "Id", id);
                    }
                    if (!Boolean.TRUE.equals(o.getIsActive())) {
                        throw new BusinessException(
                                "El servicio \"" + o.getName() + "\" ya no está disponible");
                    }
                    return new BookingCommand.Line(
                            o.getId(), o.getName(), o.getPrice(),
                            propias.getOrDefault(id, o.getDurationMinutes()),
                            BigDecimal.ZERO);
                })
                .toList();

        BusinessBookingPolicy politica = policies.forBusiness(req.businessId());

        // El canal decide que reglas aplican. Desde aqui siempre hay sesion:
        // la reserva sin cuenta entra por el controlador publico, no por este.
        AppointmentChannel canal = principal.getRoles().contains("EMPLOYEE")
                ? AppointmentChannel.APK_EMPLEADO
                : AppointmentChannel.PANEL_DUENO;

        // La confirmacion manual es para lo que pide el cliente por su cuenta.
        // Lo que agenda el propio negocio nace confirmado: nadie tiene que
        // aceptarse a si mismo una cita.
        Appointment cita = booking.book(new BookingCommand(
                req.businessId(), req.branchId(), req.employeeId(), req.businessClientId(),
                canal, principal.getUserId(), req.startUtc(), zona, lineas,
                req.backdated(), false, politica.toEnginePolicy(), Instant.now(),
                req.notes(), null));

        return ResponseEntity.ok(ApiResponse.created(toView(cita)));
    }

    /** La agenda de un dia. Es la vista del panel. */
    @GetMapping("/day")
    public ResponseEntity<ApiResponse<List<AppointmentView>>> day(
            @RequestParam UUID businessId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                conCliente(repo.byBusinessAndDay(businessId, date), businessId)));
    }

    /**
     * La agenda de un RANGO de dias. Es la vista de semana del calendario.
     *
     * <p>Una consulta por dia serviria, pero siete peticiones para pintar una
     * semana convierten cada cambio de semana en siete viajes — y en una barra
     * de carga que parpadea columna a columna.</p>
     */
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<AppointmentView>>> range(
            @RequestParam UUID businessId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {

        if (from.isAfter(to)) throw new BusinessException("El rango empieza después de terminar");
        if (from.plusDays(45).isBefore(to)) {
            // Un rango sin tope es una consulta que crece sin control. Mes y
            // medio cubre de sobra la vista de mes con sus dias de relleno.
            throw new BusinessException("El rango no puede pasar de 45 días");
        }

        List<Appointment> citas = new java.util.ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            citas.addAll(repo.byBusinessAndDay(businessId, d));
        }
        return ResponseEntity.ok(ApiResponse.success(conCliente(citas, businessId)));
    }

    /**
     * Las citas de UN empleado. Es lo unico que ve su app.
     *
     * <p>El identificador viaja por la ruta, asi que sin comprobar nada
     * cualquier empleado leeria la agenda de un companero con solo cambiarlo:
     * a quien atiende, a que hora y con que telefono. El aislamiento por
     * negocio no lo cubre —los dos son del mismo negocio—, asi que la
     * comprobacion es aqui.</p>
     *
     * <p>El dueno y el administrador pasan: la agenda del equipo es
     * literalmente su trabajo.</p>
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<AppointmentView>>> byEmployee(
            @AuthenticationPrincipal IUserPrincipal principal,
            @PathVariable UUID employeeId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {

        exigirQueSeaSuya(principal, employeeId);
        List<Appointment> citas = repo.byEmployeeBetween(employeeId, from, to);
        UUID negocio = citas.isEmpty() ? null : citas.get(0).getBusinessId();
        return ResponseEntity.ok(ApiResponse.success(conCliente(citas, negocio)));
    }

    public record StatusChangeRequest(String status, String reason) {}

    @PostMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentView>> changeStatus(
            @AuthenticationPrincipal IUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody StatusChangeRequest req) {

        AppointmentStatus destino;
        try {
            destino = AppointmentStatus.valueOf(req.status());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Ese estado no existe: " + req.status());
        }

        AppointmentChannel canal = mandaEnElNegocio(principal)
                ? AppointmentChannel.PANEL_DUENO
                : AppointmentChannel.APK_EMPLEADO;

        // El empleado solo mueve SUS citas, y solo hacia adelante: empezarla y
        // terminarla. Cancelar, rechazar o marcar inasistencia decide si a
        // alguien se le cobra o se le vuelve a reservar, y eso es del negocio.
        if (!mandaEnElNegocio(principal)) {
            Appointment cita = repo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Cita", "Id", id));
            exigirQueSeaSuya(principal, cita.getEmployeeId());
            if (destino != AppointmentStatus.EN_CURSO && destino != AppointmentStatus.COMPLETADA) {
                throw new AccessDeniedException(
                        "Desde la app solo puedes empezar y terminar tus citas. "
                                + "Para cancelarla, habla con tu jefe.");
            }
        }

        return ResponseEntity.ok(ApiResponse.success(toView(
                booking.changeStatus(id, destino, canal, principal.getUserId(),
                        req.reason(), Instant.now()))));
    }

    /**
     * Marcar inasistencia. Va aparte y con permiso propio: no es un cambio de
     * estado cualquiera, es una anotacion en el historial del cliente que puede
     * acabar bloqueandole las reservas.
     */
    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<AppointmentView>> noShow(
            @AuthenticationPrincipal IUserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success(toView(
                booking.changeStatus(id, AppointmentStatus.NO_ASISTIO,
                        AppointmentChannel.PANEL_DUENO, principal.getUserId(),
                        reason, Instant.now()))));
    }

    /**
     * Fuerza el barrido de la agenda: recordatorios y caducidad de lo pendiente.
     *
     * <p>Existe por el mismo motivo que {@code /service-charges/sync}: si el
     * servicio estuvo caido, al volver hay que poder ponerse al dia sin esperar
     * al siguiente ciclo. No es un endpoint para pruebas — es la manivela.</p>
     *
     * <p>Es idempotente: el recordatorio deja marca con clave unica antes de
     * publicar, asi que llamarlo dos veces no manda dos mensajes.</p>
     */
    @PostMapping("/sweep")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sweep() {
        sweepJob.recordar();
        sweepJob.expirar();
        return ResponseEntity.ok(ApiResponse.success(null, "Agenda al día"));
    }

    /** El historial completo de una cita: quien la movio, cuando y por que. */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> history(@PathVariable UUID id) {
        List<Map<String, Object>> out = repo.historyOf(id).stream()
                .map(h -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("from", h.getFromStatus() == null ? null : h.getFromStatus().name());
                    m.put("to", h.getToStatus().name());
                    m.put("channel", h.getChannel().name());
                    m.put("actorUserId", h.getActorUserId());
                    m.put("reason", h.getReason());
                    m.put("occurredAt", h.getOccurredAt());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(out));
    }

    /**
     * Consulta por codigo publico. La usa el cliente sin cuenta.
     *
     * <p>Pide ademas los ultimos cuatro digitos del telefono: el codigo dice
     * cual es la cita, el telefono dice que es tuya. Con el codigo solo,
     * bastaria con acertar uno para ver los datos de un desconocido.</p>
     */
    @GetMapping("/by-code/{code}")
    public ResponseEntity<ApiResponse<AppointmentView>> byPublicCode(@PathVariable String code) {
        String normalizado = PublicCode.normalize(code);
        return repo.findByPublicCode(normalizado)
                .map(a -> ResponseEntity.ok(ApiResponse.success(toView(a))))
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "código", code));
    }

    // ── Quien pregunta ────────────────────────────────────────────────────────

    /** Dueno o administrador: la agenda del equipo es su trabajo. */
    private static boolean mandaEnElNegocio(IUserPrincipal p) {
        return p.getRoles().contains("OWNER") || p.getRoles().contains("ADMIN");
    }

    /**
     * Exige que ese registro laboral sea de quien hace la peticion.
     *
     * <p>La cadena es cuenta → persona → empleado, y son dos saltos porque asi
     * esta modelado: la cuenta no sabe de sedes y el empleado no sabe de
     * contrasenas; entre medias esta la persona, que es la misma trabaje donde
     * trabaje. Una persona puede tener mas de un registro laboral (dos sedes
     * del mismo negocio), asi que se comparan todos.</p>
     */
    private void exigirQueSeaSuya(IUserPrincipal principal, UUID employeeId) {
        if (mandaEnElNegocio(principal)) return;
        if (employeeId != null && misEmpleados(principal.getUserId()).contains(employeeId)) return;
        throw new AccessDeniedException("Esa agenda no es tuya");
    }

    /** Los registros laborales de una cuenta. Vacio si la persona no existe. */
    private Set<UUID> misEmpleados(UUID userId) {
        UUID persona;
        try {
            persona = thirdParties.personByUser(userId).id();
        } catch (RuntimeException ex) {
            // Sin persona no hay empleado: la cuenta no puede ser duena de
            // ninguna agenda. Se responde 403, no 500 — no es un fallo.
            return Set.of();
        }
        return employees.findByThirdPartyId(persona).stream()
                .map(Employee::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    // ── Con quien y de que ─────────────────────────────────────────────────────

    /**
     * Anade a cada cita el cliente y los servicios.
     *
     * <p>Una fila de agenda que dice "09:00 · 45 min · $40.000" no sirve para
     * trabajar: falta a quien se atiende y que se le hace. Se resuelve en dos
     * consultas para toda la lista, no dos por fila.</p>
     */
    private List<AppointmentView> conCliente(List<Appointment> citas, UUID businessId) {
        if (citas.isEmpty()) return List.of();

        Map<UUID, BusinessClient> porId = new HashMap<>();
        if (businessId != null) {
            for (BusinessClient c : clients.findByBusinessId(businessId)) porId.put(c.getId(), c);
        }

        Map<UUID, List<AppointmentLine>> lineas = new HashMap<>();
        for (AppointmentLine l : repo.linesOfMany(citas.stream().map(Appointment::getId).toList())) {
            lineas.computeIfAbsent(l.getAppointmentId(), k -> new ArrayList<>()).add(l);
        }

        List<AppointmentView> out = new ArrayList<>(citas.size());
        for (Appointment a : citas) {
            BusinessClient cliente = porId.get(a.getBusinessClientId());
            AppointmentView base = toView(a);
            out.add(new AppointmentView(
                    base.id(), base.publicCode(), base.businessId(), base.branchId(),
                    base.employeeId(), base.businessClientId(), base.status(), base.channel(),
                    base.startUtc(), base.endUtc(), base.timeZone(), base.localDate(),
                    base.backdated(), base.totalPrice(), base.totalMinutes(), base.notes(),
                    nombreServicios(lineas.get(a.getId())),
                    cliente == null ? null : cliente.getDisplayName(),
                    cliente == null ? null : cliente.getPhoneE164()));
        }
        return out;
    }

    /** "Corte + Barba". Una cita sin lineas no deberia existir, pero no se calla. */
    private static String nombreServicios(List<AppointmentLine> lineas) {
        if (lineas == null || lineas.isEmpty()) return "Servicio";
        return String.join(" + ", lineas.stream().map(AppointmentLine::getServiceName).toList());
    }

    private static AppointmentView toView(Appointment a) {
        return new AppointmentView(
                a.getId(), a.getPublicCode(), a.getBusinessId(), a.getBranchId(),
                a.getEmployeeId(), a.getBusinessClientId(),
                a.getStatus().name(), a.getChannel().name(),
                a.getStartUtc(), a.getEndUtc(), a.getBusinessTimeZone(), a.getLocalDate(),
                a.isBackdated(), a.getTotalPrice(), a.getTotalDurationMinutes(), a.getNotes(),
                null, null, null);
    }
}
