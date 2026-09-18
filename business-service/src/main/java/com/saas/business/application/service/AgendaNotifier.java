package com.saas.business.application.service;

import com.saas.business.domain.model.Appointment;
import com.saas.business.domain.model.AppointmentLine;
import com.saas.business.domain.model.AppointmentNotice;
import com.saas.business.domain.model.Business;
import com.saas.business.domain.model.BusinessClient;
import com.saas.business.domain.model.BusinessDomain;
import com.saas.business.domain.port.out.IAppointmentNoticeRepositoryPort;
import com.saas.business.domain.port.out.IAppointmentRepositoryPort;
import com.saas.business.domain.port.out.IBusinessClientRepositoryPort;
import com.saas.business.domain.port.in.IBusinessDomainUseCase;
import com.saas.business.domain.port.out.IBusinessRepositoryPort;
import com.saas.business.domain.port.out.IEmployeeRepositoryPort;
import com.saas.business.infrastructure.client.ThirdPartyClient;
import com.saas.common.events.EventTypes;
import com.saas.common.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Los avisos de la agenda al cliente.
 *
 * <h3>Publica al outbox y se acabó</h3>
 * <p>No sabe que existen ni el correo ni WhatsApp: escribe el hecho y sus
 * parámetros, y events-service resuelve la plantilla y envía. Así cambiar el
 * texto no toca este servicio, y este servicio no se cae si el proveedor está
 * caído.</p>
 *
 * <p>Y sobre todo: NADA de lo que pasa aquí puede tumbar una reserva. Si el
 * aviso no se puede armar, se registra y se sigue — la cita ya existe, y
 * deshacerla por un mensaje sería mucho peor que un mensaje perdido.</p>
 *
 * <h3>Qué canal se usa, y por qué no lo decide events-service</h3>
 * <p>WhatsApp solo sale si el negocio lo tiene encendido Y el cliente lo aceptó
 * Y no se ha dado de baja. Eso lo sabe este servicio, no el que envía, así que
 * viaja en el evento como la lista de canales permitidos.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgendaNotifier {

    /** Los códigos de events-service. Son contrato: se siembran allí. */
    public static final String CONFIRMADA = "APPOINTMENT_CONFIRMED";
    public static final String CANCELADA = "APPOINTMENT_CANCELLED";
    public static final String RECORDATORIO = "APPOINTMENT_REMINDER";

    private static final String AGGREGATE_TYPE = "appointment";
    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final OutboxPublisher outbox;
    private final AppointmentLinkService enlaces;
    private final IBusinessDomainUseCase domains;
    private final IAppointmentRepositoryPort appointments;
    private final IAppointmentNoticeRepositoryPort notices;
    private final IBusinessClientRepositoryPort clients;
    private final IBusinessRepositoryPort businesses;
    private final IEmployeeRepositoryPort employees;
    private final ThirdPartyClient people;

    /**
     * Base pública donde el cliente consulta su cita.
     *
     * <p>Si lleva {@code {slug}}, ahí va el subdominio del negocio y el enlace
     * sale en SU dominio ({@code barberia-cc.midominio.com}). En desarrollo no
     * hay subdominios, así que el valor por defecto no lo lleva.</p>
     */
    @Value("${saas.public.base-url:http://localhost:4200}")
    private String publicBaseUrl;

    /** Lo que viaja al outbox. Espejo de lo que espera events-service. */
    private record NotificationRequest(
            String notificationCode, List<String> to, List<String> channels,
            UUID thirdPartyId, Map<String, String> data) {}

    public void confirmada(Appointment cita) {
        avisar(cita, CONFIRMADA, CONFIRMADA, null);
    }

    public void cancelada(Appointment cita, String motivo) {
        avisar(cita, CANCELADA, CANCELADA, motivo);
    }

    /**
     * Recordatorio, marcado con las horas de antelación.
     *
     * <p>La marca lleva las horas porque el de 24 y el de 2 son avisos
     * distintos: con una sola marca por cita, el segundo nunca saldría.</p>
     */
    public boolean recordatorio(Appointment cita, int horasAntes) {
        return avisar(cita, RECORDATORIO, RECORDATORIO + ":" + horasAntes, null);
    }

    /**
     * @param marca lo que se guarda para no repetir. Puede llevar sufijo (las
     *        horas del recordatorio); el código de la notificación, no.
     * @return si de verdad se publicó. Devolverlo importa: el barrido lo cuenta
     *         en su registro, y un contador que sube pase lo que pase dice que
     *         se avisó cuando no se avisó — que es peor que no contar nada.
     */
    private boolean avisar(Appointment cita, String codigo, String marca, String motivo) {
        try {
            // La marca se escribe ANTES de publicar y con clave única: si dos
            // barridos coinciden, el segundo choca contra el índice y no llega
            // a publicar. Al revés —publicar y luego marcar— el mensaje ya
            // habría salido cuando se descubre el duplicado.
            if (!notices.markIfFirst(AppointmentNotice.builder()
                    .appointmentId(cita.getId())
                    .notificationCode(marca)
                    .sentAt(LocalDateTime.now())
                    .build())) {
                return false;
            }

            BusinessClient cliente = clients.findById(cita.getBusinessClientId()).orElse(null);
            if (cliente == null) {
                log.warn("Cita {} sin cliente: no hay a quién avisar", cita.getPublicCode());
                return false;
            }

            List<String> destinos = destinos(cliente);
            if (destinos.isEmpty()) {
                // Cliente de mostrador: existe, pero no dejó por dónde escribirle.
                log.info("Cita {} sin forma de contacto: no se avisa", cita.getPublicCode());
                return false;
            }

            outbox.publish(EventTypes.NOTIFICATION_REQUESTED, cita.getBusinessId(),
                    AGGREGATE_TYPE, cita.getId(),
                    new NotificationRequest(codigo, destinos, canales(cliente),
                            cliente.getThirdPartyId(), datos(cita, cliente, motivo)));
            return true;

        } catch (Exception ex) {
            // Un aviso perdido no puede tumbar una reserva ni un barrido.
            log.warn("No se pudo publicar el aviso {} de la cita {}: {}",
                    codigo, cita.getPublicCode(), ex.getMessage());
            return false;
        }
    }

    /** Teléfono y, si la persona está registrada, su correo. */
    private List<String> destinos(BusinessClient cliente) {
        List<String> out = new ArrayList<>();
        if (cliente.getPhoneE164() != null && !cliente.getPhoneE164().isBlank()) {
            out.add(cliente.getPhoneE164());
        }
        if (cliente.getThirdPartyId() != null) {
            try {
                ThirdPartyClient.NotifyTarget t =
                        people.notifyTargets(Set.of(cliente.getThirdPartyId()))
                              .get(cliente.getThirdPartyId().toString());
                if (t != null && t.email() != null && !t.email().isBlank()) out.add(t.email());
            } catch (RuntimeException ex) {
                log.debug("Sin correo para la persona {}: {}", cliente.getThirdPartyId(), ex.getMessage());
            }
        }
        return out;
    }

    /**
     * Por dónde se le puede escribir a este cliente.
     *
     * <p>WhatsApp exige consentimiento explícito y respeta la baja. El correo y
     * el SMS salen siempre que haya dirección: son la constancia de algo que el
     * propio cliente pidió, no publicidad.</p>
     */
    private List<String> canales(BusinessClient cliente) {
        List<String> out = new ArrayList<>(List.of("EMAIL", "SMS"));
        boolean acepto = cliente.getWhatsappOptInAt() != null;
        boolean seDioDeBaja = cliente.getWhatsappOptOutAt() != null;
        if (acepto && !seDioDeBaja) out.add("WHATSAPP");
        return out;
    }

    private Map<String, String> datos(Appointment cita, BusinessClient cliente, String motivo) {
        ZoneId zona = zonaDe(cita);
        Map<String, String> d = new LinkedHashMap<>();
        d.put("CLIENTE", primerNombre(cliente.getDisplayName()));
        d.put("NEGOCIO", nombreNegocio(cita.getBusinessId()));
        d.put("SERVICIO", servicios(cita.getId()));
        d.put("EMPLEADO", nombreEmpleado(cita.getEmployeeId()));
        d.put("FECHA", fechaLarga(cita, zona));
        d.put("HORA", cita.getStartUtc().atZone(zona).format(HORA));
        d.put("CODIGO", cita.getPublicCode());
        d.put("LINK", enlaceA(cita));
        // Siempre con valor: un aviso de que algo se cayó sin explicación deja
        // al cliente llamando por teléfono.
        d.put("MOTIVO", motivo == null || motivo.isBlank() ? "No se indicó" : motivo);
        return d;
    }

    /**
     * El enlace que se le manda al cliente.
     *
     * <p>Antes era {@code /mi-cita?codigo=ABCD1234}: el codigo de la cita
     * escrito en la URL de un correo, un SMS y un WhatsApp, y de ahi al
     * historial del navegador y al registro de cualquier proxy. Ahora es un
     * token FIRMADO que caduca, y ademas va en el subdominio del negocio.</p>
     *
     * <p>Si no hay secreto configurado, se cae al camino manual en vez de
     * emitir un enlace que cualquiera podria fabricar: el cliente teclea su
     * codigo, que es mas incomodo pero no es un agujero.</p>
     */
    private String enlaceA(Appointment cita) {
        String base = baseDe(cita.getBusinessId());
        // Vale hasta bastante despues de la cita: con el se consulta, se
        // cancela y, cuando ya paso, se califica.
        String token = enlaces.emitir(cita.getId(), cita.getStartUtc().plus(Duration.ofDays(30)));
        return token == null ? base + "/mi-cita" : base + "/c/" + token;
    }

    /** La base del negocio: su subdominio si la plantilla lo pide. */
    private String baseDe(UUID businessId) {
        if (!publicBaseUrl.contains("{slug}")) return publicBaseUrl;
        String slug = domains.findByBusinessId(businessId).stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsPrimary()))
                .map(BusinessDomain::getSlug).findFirst().orElse(null);
        return slug == null
                ? publicBaseUrl.replace("{slug}.", "")
                : publicBaseUrl.replace("{slug}", slug);
    }

    /** "viernes 12 de septiembre". */
    private static String fechaLarga(Appointment cita, ZoneId zona) {
        var f = cita.getStartUtc().atZone(zona).toLocalDate();
        return f.getDayOfWeek().getDisplayName(TextStyle.FULL, ES) + " "
                + f.getDayOfMonth() + " de " + f.getMonth().getDisplayName(TextStyle.FULL, ES);
    }

    private static ZoneId zonaDe(Appointment cita) {
        try {
            return ZoneId.of(cita.getBusinessTimeZone() == null
                    ? "America/Bogota" : cita.getBusinessTimeZone());
        } catch (RuntimeException ex) {
            return ZoneId.of("America/Bogota");
        }
    }

    /** El nombre de pila basta y cabe en un SMS. */
    private static String primerNombre(String completo) {
        if (completo == null || completo.isBlank()) return "Hola";
        String primero = completo.trim().split("\\s+")[0];
        return primero.isBlank() ? "Hola" : primero;
    }

    private String nombreNegocio(UUID businessId) {
        Business b = businesses.findById(businessId).orElse(null);
        if (b == null) return "tu negocio";
        return b.getTradeName() != null && !b.getTradeName().isBlank()
                ? b.getTradeName() : b.getName();
    }

    /** "Corte + Barba". */
    private String servicios(UUID appointmentId) {
        List<AppointmentLine> lineas = appointments.linesOf(appointmentId);
        if (lineas == null || lineas.isEmpty()) return "tu servicio";
        return String.join(" + ", lineas.stream().map(AppointmentLine::getServiceName).toList());
    }

    private String nombreEmpleado(UUID employeeId) {
        try {
            var e = employees.findById(employeeId).orElse(null);
            if (e == null || e.getThirdPartyId() == null) return "tu profesional";
            String nombre = people.personNames(Set.of(e.getThirdPartyId()))
                    .get(e.getThirdPartyId().toString());
            return nombre == null || nombre.isBlank() ? "tu profesional" : nombre;
        } catch (RuntimeException ex) {
            return "tu profesional";
        }
    }
}
