package com.saas.business.infrastructure.controller;

import com.saas.business.application.service.AvailabilityQueryService;
import com.saas.business.application.service.AppointmentBookingService;
import com.saas.business.application.service.BookingCommand;
import com.saas.business.domain.model.Appointment;
import com.saas.business.domain.model.AppointmentChannel;
import com.saas.business.domain.model.AppointmentStatus;
import com.saas.business.domain.model.Branch;
import com.saas.business.domain.model.Business;
import com.saas.business.domain.model.BusinessBookingPolicy;
import com.saas.business.domain.model.BusinessClient;
import com.saas.business.domain.model.BusinessDomain;
import com.saas.business.domain.model.CancelledBy;
import com.saas.business.domain.model.Employee;
import com.saas.business.domain.model.Offering;
import com.saas.business.domain.model.PublicCode;
import com.saas.business.domain.port.in.IBranchUseCase;
import com.saas.business.domain.port.in.IBusinessBookingPolicyUseCase;
import com.saas.business.domain.port.in.IBusinessClientUseCase;
import com.saas.business.domain.port.in.IBusinessDomainUseCase;
import com.saas.business.domain.port.in.IBusinessUseCase;
import com.saas.business.domain.port.in.IEmployeeUseCase;
import com.saas.business.domain.port.in.IOfferingUseCase;
import com.saas.business.domain.port.out.IAppointmentRepositoryPort;
import com.saas.business.application.service.AppointmentLinkService;
import com.saas.business.domain.model.OfferingCategory;
import com.saas.business.domain.port.out.IEmployeeOfferingRepositoryPort;
import com.saas.business.domain.port.out.IOfferingCategoryRepositoryPort;
import com.saas.business.infrastructure.client.ThirdPartyClient;
import com.saas.common.dto.ApiResponse;
import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.util.PhoneNumbers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reservar desde la web del negocio, sin cuenta.
 *
 * <p>Cuelga de {@code /business/public}, que el gateway deja pasar sin token, y
 * el negocio se resuelve por su SLUG —el subdominio—, nunca por un id del
 * cuerpo. Eso es lo que impide que alguien reserve en la agenda de otro negocio
 * cambiando un UUID.</p>
 *
 * <h3>Lo que este controlador no hace</h3>
 * <p>No calcula disponibilidad ni decide si un hueco esta libre: eso es del
 * motor y del servicio de reservas, con su cerrojo. Aqui solo se resuelve quien
 * pregunta y se traduce a lo que el dominio entiende.</p>
 */
@Slf4j
@RestController
@RequestMapping("/public/booking")
@RequiredArgsConstructor
public class PublicBookingController {

    private final IBusinessDomainUseCase domains;
    private final IBusinessUseCase businesses;
    private final IBranchUseCase branches;
    private final IOfferingUseCase offerings;
    private final IOfferingCategoryRepositoryPort categories;
    private final AppointmentLinkService enlaces;
    private final IEmployeeUseCase employees;
    private final IEmployeeOfferingRepositoryPort employeeOfferings;
    private final IBusinessBookingPolicyUseCase policies;
    private final IBusinessClientUseCase clients;
    private final AvailabilityQueryService availability;
    private final AppointmentBookingService booking;
    private final IAppointmentRepositoryPort appointments;
    private final ThirdPartyClient thirdParty;

    // -----------------------------------------------------------------
    // Contexto
    // -----------------------------------------------------------------

    public record BranchView(UUID id, String name, String address, String phone) {}
    /**
     * Un servicio del catalogo, con SU categoria resuelta.
     *
     * <p>El nombre de la categoria viaja ya resuelto porque la pantalla agrupa
     * por ella: mandar solo el id obligaria a una segunda peticion —publica y
     * sin sesion— solo para poder escribir una etiqueta.</p>
     */
    public record OfferingView(UUID id, String name, String description,
                               UUID categoryId, String categoryName,
                               BigDecimal price, Integer durationMinutes) {}
    public record ProfessionalView(UUID id, String name, String photoUrl) {}

    /**
     * Todo lo que la pagina necesita para empezar, en una peticion.
     *
     * @param requiresManualConfirmation la reserva nace pendiente y alguien del
     *        negocio tiene que aceptarla. La pantalla lo dice ANTES de
     *        reservar: "queda pendiente" y "queda confirmada" no son lo mismo
     *        para quien esta reservando.
     * @param requiresPhoneVerification hay que verificar el telefono con un
     *        codigo. Ver {@link #exigeVerificacion(BusinessBookingPolicy)}.
     */
    public record BookingContext(
            UUID businessId, String name, String slug, String timeZone,
            List<BranchView> branches, List<OfferingView> offerings,
            boolean requiresManualConfirmation, int minLeadTimeMinutes,
            int maxHorizonDays, int clientCancelWindowHours,
            boolean requiresPhoneVerification) {}

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<BookingContext>> context(@PathVariable String slug) {
        UUID businessId = businessIdOf(slug);
        Business negocio = businesses.getById(businessId);
        BusinessBookingPolicy p = policies.forBusiness(businessId);

        List<BranchView> sedes = branches.findByBusiness(businessId).stream()
                .filter(b -> !Boolean.FALSE.equals(b.getEnabled()))
                .map(b -> new BranchView(b.getId(), b.getName(), b.getAddressLine(), b.getPhone()))
                .toList();

        Map<UUID, String> categorias = categories.findByBusinessId(businessId).stream()
                .filter(c -> !Boolean.FALSE.equals(c.getVisible()))
                .collect(Collectors.toMap(OfferingCategory::getId, OfferingCategory::getName,
                        (a, b) -> a));

        List<OfferingView> servicios = offerings.findByBusiness(businessId).stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsActive()))
                .map(o -> new OfferingView(o.getId(), o.getName(), o.getDescription(),
                        o.getCategoryId(),
                        o.getCategoryId() == null ? null : categorias.get(o.getCategoryId()),
                        o.getPrice(), o.getDurationMinutes()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(new BookingContext(
                businessId, negocio.getName(), slug,
                negocio.getTimeZone() == null ? "America/Bogota" : negocio.getTimeZone(),
                sedes, servicios,
                Boolean.TRUE.equals(p.getRequiresManualConfirmation()),
                p.getMinLeadTimeMinutes(), p.getMaxHorizonDays(),
                p.getClientCancelWindowHours(),
                exigeVerificacion(p))));
    }

    /**
     * Si hay que verificar el telefono ANTES de reservar.
     *
     * <p>No basta con que el negocio lo pida: hace falta que exista un canal
     * por donde mandar el codigo. Pedir verificacion sin poder verificar dejaba
     * la reserva publica MUERTA — y era el caso por defecto, porque la columna
     * nace en TRUE y ningun negocio tiene WhatsApp dado de alta todavia.</p>
     *
     * <p>Lo usan los DOS sitios que deciden sobre esto, y por eso esta aqui y
     * no repetido: el contexto, para que la pantalla esconda el paso, y el POST
     * de reserva, que es donde la regla se hace cumplir de verdad. Cuando solo
     * lo sabia el contexto, la pantalla escondia el paso y el servidor seguia
     * respondiendo 400 al final — que es peor que el problema original, porque
     * el fallo aparecia despues de rellenarlo todo.</p>
     *
     * <p>SMS no cuenta porque no hay proveedor conectado. Cuando lo haya se
     * anade aqui, en un solo sitio, y la reserva empieza a pedir codigo sola.</p>
     */
    private boolean exigeVerificacion(BusinessBookingPolicy p) {
        boolean loPide = !Boolean.FALSE.equals(p.getRequirePhoneVerification());
        boolean hayCanal = Boolean.TRUE.equals(p.getWhatsappEnabled()) && p.getWhatsappPhoneId() != null;
        return loPide && hayCanal;
    }

    /**
     * Quien puede atender esos servicios en esa sede.
     *
     * <p>Solo quien presta TODOS los pedidos: una cita de corte + barba con
     * quien solo hace corte no es media cita, es una cita imposible.</p>
     */
    @GetMapping("/{slug}/professionals")
    public ResponseEntity<ApiResponse<List<ProfessionalView>>> professionals(
            @PathVariable String slug,
            @RequestParam UUID branchId,
            @RequestParam List<UUID> offeringIds) {

        UUID businessId = businessIdOf(slug);
        exigirSedeDelNegocio(businessId, branchId);

        Map<UUID, Employee> candidatos = employees.findByBranch(branchId).stream()
                .filter(e -> !Boolean.FALSE.equals(e.getEnabled()))
                .filter(e -> e.getTerminationDate() == null
                        || e.getTerminationDate().isAfter(LocalDate.now()))
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));

        Map<UUID, Set<UUID>> serviciosPorEmpleado = new HashMap<>();
        employeeOfferings.findByOfferingIds(offeringIds).stream()
                .filter(eo -> candidatos.containsKey(eo.getEmployeeId()))
                .filter(eo -> !Boolean.FALSE.equals(eo.getEnabled()))
                .forEach(eo -> serviciosPorEmpleado
                        .computeIfAbsent(eo.getEmployeeId(), k -> new HashSet<>())
                        .add(eo.getOfferingId()));

        List<Employee> elegibles = serviciosPorEmpleado.entrySet().stream()
                .filter(e -> e.getValue().containsAll(offeringIds))
                .map(e -> candidatos.get(e.getKey()))
                .toList();
        if (elegibles.isEmpty()) return ResponseEntity.ok(ApiResponse.success(List.of()));

        // Los nombres viven en personas, no aqui. Se piden en lote.
        Map<String, ThirdPartyClient.PersonCard> fichas = tarjetas(elegibles);

        List<ProfessionalView> out = elegibles.stream().map(e -> {
            ThirdPartyClient.PersonCard ficha = e.getThirdPartyId() == null
                    ? null : fichas.get(e.getThirdPartyId().toString());
            return new ProfessionalView(e.getId(), nombreOGenerico(ficha),
                    ficha == null ? null : ficha.photoUrl());
        }).toList();
        return ResponseEntity.ok(ApiResponse.success(out));
    }

    /** Los huecos libres. Mismo motor que el panel: no hay un cálculo "público". */
    @GetMapping("/{slug}/availability")
    public ResponseEntity<ApiResponse<AvailabilityController.AvailabilityView>> availability(
            @PathVariable String slug,
            @RequestParam UUID branchId,
            @RequestParam List<UUID> offeringIds,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        UUID businessId = businessIdOf(slug);
        exigirSedeDelNegocio(businessId, branchId);

        AvailabilityQueryService.Result r = availability.slots(new AvailabilityQueryService.Query(
                businessId, branchId, offeringIds, employeeId, from, to));

        List<AvailabilityController.SlotView> vista = r.slots().stream()
                .map(s -> new AvailabilityController.SlotView(
                        s.startUtc(), s.endUtc(), s.startUtc().atZone(r.zone()).toLocalDate(),
                        s.employeeId(), Duration.between(s.startUtc(), s.endUtc()).toMinutes()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(
                new AvailabilityController.AvailabilityView(r.zone().getId(), vista)));
    }

    // -----------------------------------------------------------------
    // Reservar
    // -----------------------------------------------------------------

    /**
     * @param whatsappOptIn aceptacion explicita de recibir mensajes. Se guarda
     *        con la fecha y el texto que la persona vio, porque un opt-in sin
     *        constancia de que se acepto no es un opt-in.
     */
    public record PublicBookingRequest(
            @NotNull UUID branchId,
            @NotNull UUID employeeId,
            @NotNull Instant startUtc,
            @NotEmpty List<UUID> offeringIds,
            @NotBlank @Size(max = 160) String clientName,
            @NotBlank @Size(max = 30) String clientPhone,
            Boolean whatsappOptIn,
            @Size(max = 300) String whatsappOptInText,
            @Size(max = 500) String notes) {}

    /** Lo que se le devuelve a quien reservo: su codigo y poco mas. */
    public record BookingResult(String publicCode, String status, Instant startUtc,
                                String timeZone, String employeeName, BigDecimal totalPrice) {}

    @PostMapping("/{slug}/appointments")
    public ResponseEntity<ApiResponse<BookingResult>> book(
            @PathVariable String slug,
            @Valid @RequestBody PublicBookingRequest req) {

        UUID businessId = businessIdOf(slug);
        exigirSedeDelNegocio(businessId, req.branchId());

        String telefono = PhoneNumbers.toE164(req.clientPhone());
        if (telefono == null) {
            throw new BusinessException("Ese número de celular no parece válido");
        }

        Business negocio = businesses.getById(businessId);
        ZoneId zona = zonaDe(negocio);
        BusinessBookingPolicy politica = policies.forBusiness(businessId);

        BusinessClient cliente = clients.findOrCreateByPhone(businessId, telefono, req.clientName());

        // El teléfono verificado se exige AQUI, no en la pantalla: la misma
        // regla vale para la web, para WhatsApp y para cualquier integración.
        if (exigeVerificacion(politica) && cliente.getPhoneVerifiedAt() == null) {
            throw new BusinessException(
                    "Verifica tu número antes de reservar: te enviamos un código.");
        }

        // Por su operacion propia, no por la edicion de ficha: applyChanges no
        // toca las marcas de consentimiento a proposito.
        if (Boolean.TRUE.equals(req.whatsappOptIn())) {
            clients.recordWhatsappOptIn(cliente.getId(), req.whatsappOptInText());
        }

        Map<UUID, Offering> catalogo = offerings.findByBusiness(businessId).stream()
                .collect(Collectors.toMap(Offering::getId, o -> o));

        // La duracion propia del empleado manda sobre la del catalogo, igual
        // que en el panel: si aqui se usara la del catalogo, la cita duraria
        // menos de lo que ocupa y la agenda se solaparia sola.
        Map<UUID, Integer> propias = employeeOfferings.findByOfferingIds(req.offeringIds()).stream()
                .filter(eo -> req.employeeId().equals(eo.getEmployeeId()))
                .filter(eo -> eo.getDurationMinutes() != null && eo.getDurationMinutes() > 0)
                .collect(Collectors.toMap(eo -> eo.getOfferingId(),
                        eo -> eo.getDurationMinutes(), (a, b) -> a));

        List<BookingCommand.Line> lineas = new ArrayList<>();
        for (UUID id : req.offeringIds()) {
            Offering o = catalogo.get(id);
            if (o == null) throw new ResourceNotFoundException("Servicio", "Id", id);
            if (!Boolean.TRUE.equals(o.getIsActive())) {
                throw new BusinessException("El servicio \"" + o.getName() + "\" ya no está disponible");
            }
            lineas.add(new BookingCommand.Line(o.getId(), o.getName(), o.getPrice(),
                    propias.getOrDefault(id, o.getDurationMinutes()), BigDecimal.ZERO));
        }

        Appointment cita = booking.book(new BookingCommand(
                businessId, req.branchId(), req.employeeId(), cliente.getId(),
                AppointmentChannel.WEB_PUBLICA, null, req.startUtc(), zona, lineas,
                false, Boolean.TRUE.equals(politica.getRequiresManualConfirmation()),
                politica.toEnginePolicy(), Instant.now(), req.notes(), null));

        return ResponseEntity.ok(ApiResponse.created(new BookingResult(
                cita.getPublicCode(), cita.getStatus().name(), cita.getStartUtc(),
                cita.getBusinessTimeZone(), nombreDe(req.employeeId()), cita.getTotalPrice())));
    }

    // -----------------------------------------------------------------
    // Consultar y cancelar sin cuenta
    // -----------------------------------------------------------------

    /**
     * Consulta por codigo publico + los ultimos cuatro digitos del telefono.
     *
     * <p>Los dos datos, no uno: el codigo dice CUAL es la cita, el telefono
     * dice que es TUYA. Con el codigo solo, bastaria con acertar uno para leer
     * los datos de un desconocido.</p>
     */
    public record LookupRequest(@NotBlank @Size(max = 8) String last4) {}

    /**
     * Es POST y no GET a proposito, y el telefono va en el CUERPO.
     *
     * <p>Antes iba como {@code ?last4=1234}, y un query string acaba escrito en
     * el registro de accesos del servidor, en el historial del navegador y en
     * cualquier proxy intermedio. Cuatro digitos del telefono de una persona no
     * pueden quedar ahi. Un GET con cuerpo no lo permite el estandar, asi que
     * la consulta se hace por POST: no crea nada, pero es la unica forma de que
     * el dato no viaje en la URL.</p>
     */
    @PostMapping("/{slug}/appointments/{code}/lookup")
    public ResponseEntity<ApiResponse<BookingResult>> byCode(
            @PathVariable String slug, @PathVariable String code,
            @Valid @RequestBody LookupRequest req) {
        return ResponseEntity.ok(ApiResponse.success(vistaDe(citaDe(slug, code, req.last4()))));
    }

    /**
     * La misma cita, pero por el enlace opaco que se le mando al cliente.
     *
     * <p>Aqui NO se pide el telefono: el token ya es la credencial, va firmado
     * y caduca. Es lo que permite que el aviso lleve un enlace en el que se
     * pulsa y ya, sin teclear un codigo, y sin que la URL cuente nada de quien
     * la abre.</p>
     */
    @GetMapping("/{slug}/appointments/by-token/{token}")
    public ResponseEntity<ApiResponse<BookingResult>> byToken(
            @PathVariable String slug, @PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(vistaDe(citaDeToken(slug, token))));
    }

    public record CancelByTokenRequest(@Size(max = 300) String reason) {}

    @PostMapping("/{slug}/appointments/by-token/{token}/cancel")
    public ResponseEntity<ApiResponse<BookingResult>> cancelByToken(
            @PathVariable String slug, @PathVariable String token,
            @Valid @RequestBody CancelByTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                cancelar(citaDeToken(slug, token), req.reason()), "Cita cancelada"));
    }

    public record CancelRequest(@NotBlank String last4, @Size(max = 300) String reason) {}

    @PostMapping("/{slug}/appointments/{code}/cancel")
    public ResponseEntity<ApiResponse<BookingResult>> cancel(
            @PathVariable String slug, @PathVariable String code,
            @Valid @RequestBody CancelRequest req) {

        return ResponseEntity.ok(ApiResponse.success(
                cancelar(citaDe(slug, code, req.last4()), req.reason()), "Cita cancelada"));
    }

    /** La ventana de cancelacion la pone el negocio, y se comprueba AQUI. */
    private BookingResult cancelar(Appointment cita, String motivo) {
        BusinessBookingPolicy politica = policies.forBusiness(cita.getBusinessId());

        int horas = politica.getClientCancelWindowHours() == null
                ? 0 : politica.getClientCancelWindowHours();
        Instant limite = cita.getStartUtc().minus(Duration.ofHours(horas));
        if (Instant.now().isAfter(limite)) {
            throw new BusinessException("Ya no se puede cancelar por aquí: quedan menos de "
                    + horas + " horas. Llama al negocio y lo arreglan contigo.");
        }

        return vistaDe(booking.changeStatus(cita.getId(),
                AppointmentStatus.CANCELADA_CLIENTE, AppointmentChannel.WEB_PUBLICA,
                null, motivo, Instant.now()));
    }

    /**
     * La cita del token, comprobando ademas que sea de ESE negocio.
     *
     * <p>Sin esa comprobacion, un token valido serviria desde el subdominio de
     * cualquier otro negocio y le ensenaria los datos de un cliente ajeno.</p>
     */
    private Appointment citaDeToken(String slug, String token) {
        UUID businessId = businessIdOf(slug);
        Appointment cita = appointments.findById(enlaces.leer(token)).orElse(null);
        if (cita == null || !businessId.equals(cita.getBusinessId())) {
            throw new BusinessException("Ese enlace ya no sirve. Consulta tu cita con el código.");
        }
        return cita;
    }

    // -----------------------------------------------------------------
    // Apoyo
    // -----------------------------------------------------------------

    /** El negocio sale del SLUG. Nunca del cuerpo de la peticion. */
    private UUID businessIdOf(String slug) {
        BusinessDomain d = domains.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "slug", slug));
        return d.getBusinessId();
    }

    /**
     * La sede tiene que ser de ese negocio.
     *
     * <p>Sin esta comprobacion, el slug elegiria el negocio pero el id de sede
     * podria apuntar a otro, y la reserva acabaria en la agenda equivocada.</p>
     */
    private void exigirSedeDelNegocio(UUID businessId, UUID branchId) {
        Branch sede = branches.getById(branchId);
        if (sede == null || !businessId.equals(sede.getBusinessId())) {
            throw new ResourceNotFoundException("Sede", "Id", branchId);
        }
    }

    private Appointment citaDe(String slug, String code, String last4) {
        UUID businessId = businessIdOf(slug);
        Appointment cita = appointments.findByPublicCode(PublicCode.normalize(code))
                .filter(a -> businessId.equals(a.getBusinessId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "código", code));

        BusinessClient cliente = clients.getById(cita.getBusinessClientId());
        String suyos = PhoneNumbers.last4(cliente == null ? null : cliente.getPhoneE164());
        if (suyos == null || !suyos.equals(last4 == null ? null : last4.trim())) {
            // Mismo error que si no existiera: decir "el codigo esta bien pero
            // el telefono no" confirma que esa cita existe.
            throw new ResourceNotFoundException("Cita", "código", code);
        }
        return cita;
    }

    private BookingResult vistaDe(Appointment a) {
        return new BookingResult(a.getPublicCode(), a.getStatus().name(), a.getStartUtc(),
                a.getBusinessTimeZone(), nombreDe(a.getEmployeeId()), a.getTotalPrice());
    }

    private Map<String, ThirdPartyClient.PersonCard> tarjetas(List<Employee> lista) {
        Set<UUID> ids = lista.stream().map(Employee::getThirdPartyId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        try {
            return thirdParty.personCards(ids);
        } catch (RuntimeException ex) {
            // Que no se pueda pintar un nombre no puede tumbar la pagina de
            // reservas: se agenda igual, con el nombre generico.
            log.warn("No se pudieron resolver los nombres de los profesionales: {}", ex.toString());
            return Map.of();
        }
    }

    private String nombreDe(UUID employeeId) {
        Employee e = employees.getById(employeeId);
        if (e == null || e.getThirdPartyId() == null) return "Profesional";
        return nombreOGenerico(tarjetas(List.of(e)).get(e.getThirdPartyId().toString()));
    }

    /**
     * El nombre, o "Profesional" si no hay.
     *
     * <p>Tambien cuando viene VACIO, no solo cuando falta la ficha: una persona
     * dada de alta sin nombre dejaria un hueco en la lista de la web publica, y
     * un hueco se lee como un fallo de la pagina.</p>
     */
    private static String nombreOGenerico(ThirdPartyClient.PersonCard ficha) {
        if (ficha == null || ficha.fullName() == null || ficha.fullName().isBlank()) {
            return "Profesional";
        }
        return ficha.fullName();
    }

    private static ZoneId zonaDe(Business negocio) {
        try {
            return ZoneId.of(negocio.getTimeZone() == null ? "America/Bogota" : negocio.getTimeZone());
        } catch (RuntimeException ex) {
            return ZoneId.of("America/Bogota");
        }
    }
}
