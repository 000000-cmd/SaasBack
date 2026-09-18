package com.saas.business.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saas.business.domain.model.Appointment;
import com.saas.business.domain.model.AppointmentChannel;
import com.saas.business.domain.model.Business;
import com.saas.business.domain.model.BusinessBookingPolicy;
import com.saas.business.domain.model.BusinessClient;
import com.saas.business.domain.model.Employee;
import com.saas.business.domain.model.EmployeeOffering;
import com.saas.business.domain.model.Offering;
import com.saas.business.domain.model.WhatsappSession;
import com.saas.business.domain.port.in.IBusinessBookingPolicyUseCase;
import com.saas.business.domain.port.in.IBusinessClientUseCase;
import com.saas.business.domain.port.in.IBusinessUseCase;
import com.saas.business.domain.port.in.IOfferingUseCase;
import com.saas.business.domain.port.out.IBranchRepositoryPort;
import com.saas.business.domain.port.out.IEmployeeOfferingRepositoryPort;
import com.saas.business.domain.port.out.IEmployeeRepositoryPort;
import com.saas.business.domain.port.out.IWhatsappRepositoryPort;
import com.saas.business.infrastructure.client.SystemClient;
import com.saas.business.infrastructure.client.ThirdPartyClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
 * Reservar conversando por WhatsApp.
 *
 * <h3>Los pasos NO están escritos aquí</h3>
 * <p>Salen del flujo configurado, resuelto para el canal {@code WHATSAPP}, y se
 * reconocen por su CÓDIGO ({@code SERVIC}, {@code PROFES}…). Si el dueño quita
 * el paso del profesional, el bot deja de preguntarlo sin tocar una línea. Un
 * paso cuyo código este servicio no conoce se salta, igual que hace el
 * asistente de la web: una configuración nueva no puede dejar el canal mudo.</p>
 *
 * <h3>Los textos tampoco</h3>
 * <p>Vienen de {@code flow_message} por código. Cambiar cómo saluda el negocio
 * es editar una fila, no desplegar.</p>
 *
 * <h3>Por qué en WhatsApp no hay verificación de teléfono</h3>
 * <p>Porque ya está verificado por construcción: el mensaje llegó DESDE ese
 * número, y quien lo entrega es Meta. Pedir un código por SMS a alguien que te
 * está escribiendo desde su propio WhatsApp es pedirle que demuestre lo que
 * acaba de demostrar. El paso {@code VERIFI} se salta en este canal aunque la
 * política del negocio lo exija — y esa es la única excepción, escrita aquí y
 * no repartida.</p>
 *
 * <h3>Números, no texto libre</h3>
 * <p>Cada pregunta ofrece una lista numerada y espera un número. Interpretar
 * lenguaje libre es un proyecto aparte, y uno que se equivoca reservando la
 * hora que no era. Lo que no se entiende se vuelve a preguntar.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappConversationService {

    /** Cuánto vive una conversación a medias. Pasado eso, "hola" empieza de cero. */
    private static final int VIGENCIA_HORAS = 6;

    /** Cuántas opciones caben en un mensaje sin que nadie las lea en diagonal. */
    private static final int MAX_OPCIONES = 8;

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final IWhatsappRepositoryPort repo;
    private final IBusinessUseCase businesses;
    private final IBusinessBookingPolicyUseCase policies;
    private final IOfferingUseCase offerings;
    private final IBranchRepositoryPort branches;
    private final IEmployeeRepositoryPort employees;
    private final IEmployeeOfferingRepositoryPort employeeOfferings;
    private final IBusinessClientUseCase clients;
    private final AvailabilityQueryService availability;
    private final AppointmentBookingService booking;
    private final SystemClient system;
    private final ThirdPartyClient people;
    private final ObjectMapper json;

    /**
     * Contesta a un mensaje. Devuelve lo que hay que responder.
     *
     * <p>Nunca lanza hacia arriba: el webhook tiene que responder 200 pase lo
     * que pase, o Meta reintenta en bucle. Un fallo aquí sale como un mensaje
     * que el cliente entiende y queda en el registro.</p>
     */
    @Transactional
    public String responder(UUID businessId, String phone, String texto) {
        try {
            return conversar(businessId, phone, texto == null ? "" : texto.trim());
        } catch (RuntimeException ex) {
            log.warn("Conversación de WhatsApp rota para {}: {}", phone, ex.getMessage());
            // Se corta la conversación: seguir desde un estado que no se sabe
            // cuál es acaba reservando cualquier cosa.
            repo.dropSession(businessId, phone);
            return texto(businessId, "AGEND_ERROR",
                    "Se me enredó algo. Escríbeme \"hola\" y empezamos de nuevo.");
        }
    }

    // =================================================================
    // La conversación
    // =================================================================

    private String conversar(UUID businessId, String phone, String texto) {
        BusinessBookingPolicy politica = policies.forBusiness(businessId);
        if (!Boolean.TRUE.equals(politica.getWhatsappEnabled())) {
            return texto(businessId, "AGEND_NEGOCIO_CERRADO",
                    "Este negocio no atiende por WhatsApp. Escríbeles directamente.");
        }

        WhatsappSession sesion = repo.session(businessId, phone)
                .filter(s -> s.getExpiresAt() != null
                        && s.getExpiresAt().isAfter(java.time.LocalDateTime.now()))
                .orElse(null);

        // Reiniciar es una salida que siempre tiene que existir: quien se
        // atasca escribe "hola" y vuelve a empezar.
        if (sesion == null || esReinicio(texto)) {
            sesion = nueva(businessId, phone);
            return preguntar(businessId, sesion, primerPaso(businessId));
        }

        // Con paso en curso, el texto es la RESPUESTA a ese paso.
        String siguiente = aplicar(businessId, sesion, texto);
        return siguiente;
    }

    /** Los pasos que este canal ve, en orden, y que el bot sabe conducir. */
    private List<String> pasos(UUID businessId) {
        List<String> conocidos = List.of("SERVIC", "PROFES", "FECHOR", "DATBAS", "CONFIR");
        try {
            SystemClient.ResolvedFlow flujo = system.resolvedFlow("AGEND", "WHATSAPP").data();
            if (flujo == null || flujo.sections() == null) return conocidos;
            List<String> out = new ArrayList<>();
            for (SystemClient.ResolvedFlow.Section s : flujo.sections()) {
                // VERIFI se salta SIEMPRE en este canal: el número ya está
                // verificado por el propio WhatsApp (ver javadoc de la clase).
                if ("VERIFI".equals(s.code())) continue;
                // Un paso cuyo código no se sabe conducir se salta, en vez de
                // dejar la conversación parada en algo que nadie puede
                // contestar. Es la misma regla del asistente de la web.
                if (conocidos.contains(s.code())) out.add(s.code());
            }
            return out.isEmpty() ? conocidos : out;
        } catch (RuntimeException ex) {
            // Sin system-service el bot sigue funcionando con los pasos de
            // siempre: quedarse mudo por un fallo de red sería peor.
            log.warn("Sin flujo configurado, se usan los pasos por defecto: {}", ex.getMessage());
            return conocidos;
        }
    }

    private String primerPaso(UUID businessId) {
        return pasos(businessId).get(0);
    }

    private String pasoTras(UUID businessId, String actual) {
        List<String> ps = pasos(businessId);
        int i = ps.indexOf(actual);
        return (i < 0 || i + 1 >= ps.size()) ? null : ps.get(i + 1);
    }

    // =================================================================
    // Preguntar
    // =================================================================

    /** Formula el paso y deja en la sesión las opciones que ofreció. */
    private String preguntar(UUID businessId, WhatsappSession s, String paso) {
        ObjectNode datos = datos(s);
        s.setStepCode(paso);

        String salida = switch (paso) {
            case "SERVIC" -> pedirServicio(businessId, datos);
            case "PROFES" -> pedirProfesional(businessId, datos);
            case "FECHOR" -> pedirHora(businessId, datos);
            case "DATBAS" -> texto(businessId, "AGEND_PIDE_NOMBRE", "¿A nombre de quién la reservo?");
            case "CONFIR" -> resumen(businessId, datos);
            default -> texto(businessId, "AGEND_ERROR", "No sé qué preguntarte ahora.");
        };

        s.setDataJson(datos.toString());
        repo.saveSession(s);
        return salida;
    }

    private String pedirServicio(UUID businessId, ObjectNode datos) {
        List<Offering> lista = offerings.findByBusiness(businessId).stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsActive()))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .limit(MAX_OPCIONES)
                .toList();
        if (lista.isEmpty()) {
            return texto(businessId, "AGEND_NEGOCIO_CERRADO",
                    "Este negocio todavía no tiene servicios publicados.");
        }
        guardarOpciones(datos, lista.stream().map(o -> o.getId().toString()).toList());

        StringBuilder sb = new StringBuilder(
                texto(businessId, "AGEND_SALUDO", "Hola. Te ayudo a reservar tu cita en {{NEGOCIO}}."));
        sb.append("\n");
        int i = 1;
        for (Offering o : lista) {
            sb.append("\n").append(i++).append(". ").append(o.getName())
              .append(" · ").append(o.getDurationMinutes()).append(" min · ")
              .append(pesos(o.getPrice()));
        }
        return conNegocio(businessId, sb.toString());
    }

    private String pedirProfesional(UUID businessId, ObjectNode datos) {
        List<UUID> servicios = uuids(datos, "offeringIds");
        List<Employee> equipo = empleadosQueHacen(businessId, servicios).stream()
                .limit(MAX_OPCIONES).toList();

        // "Cualquiera" siempre es la opción 0: es la que más se elige y la que
        // más huecos abre.
        List<String> ids = new ArrayList<>();
        ids.add("");
        equipo.forEach(e -> ids.add(e.getId().toString()));
        guardarOpciones(datos, ids);

        Map<String, String> nombres = nombresDe(equipo);
        StringBuilder sb = new StringBuilder(texto(businessId, "AGEND_PIDE_PROFESIONAL",
                "¿Con quién prefieres? Si te da igual, responde 1."));
        sb.append("\n\n1. Cualquiera");
        int i = 2;
        for (Employee e : equipo) {
            sb.append("\n").append(i++).append(". ")
              .append(nombres.getOrDefault(String.valueOf(e.getThirdPartyId()), "Profesional"));
        }
        return sb.toString();
    }

    private String pedirHora(UUID businessId, ObjectNode datos) {
        List<UUID> servicios = uuids(datos, "offeringIds");
        UUID empleado = uuidOrNull(datos, "employeeId");
        UUID sede = sedePrincipal(businessId);
        if (sede == null) {
            return texto(businessId, "AGEND_NEGOCIO_CERRADO",
                    "Este negocio todavía no tiene la agenda disponible.");
        }

        LocalDate desde = LocalDate.now();
        AvailabilityQueryService.Result r = availability.slots(new AvailabilityQueryService.Query(
                businessId, sede, servicios, empleado, desde, desde.plusDays(7)));

        List<com.saas.business.domain.availability.Slot> huecos = r.slots().stream().limit(MAX_OPCIONES).toList();
        if (huecos.isEmpty()) {
            return texto(businessId, "AGEND_SIN_HUECOS",
                    "No queda ningún hueco esta semana. Dime otra fecha y lo miramos.");
        }

        // Se guarda el hueco ENTERO (instante + empleado): con solo la hora,
        // al confirmar habría que volver a resolver quién lo atiende y podría
        // salir otro.
        List<String> ids = huecos.stream()
                .map(h -> h.startUtc().toString() + "|" + h.employeeId()).toList();
        guardarOpciones(datos, ids);

        StringBuilder sb = new StringBuilder(texto(businessId, "AGEND_PIDE_HORA",
                "Estas son las horas libres. Responde con el número de la que quieras."));
        sb.append("\n");
        int i = 1;
        for (com.saas.business.domain.availability.Slot h : huecos) {
            sb.append("\n").append(i++).append(". ").append(cuando(h.startUtc(), r.zone()));
        }
        return sb.toString();
    }

    private String resumen(UUID businessId, ObjectNode datos) {
        String hueco = datos.path("startUtc").asText("");
        ZoneId zona = zonaDe(businessId);
        StringBuilder sb = new StringBuilder("Resumen de tu cita:\n");
        sb.append("\n• ").append(nombresServicios(businessId, uuids(datos, "offeringIds")));
        sb.append("\n• ").append(hueco.isEmpty() ? "—" : cuando(Instant.parse(hueco), zona));
        sb.append("\n• A nombre de ").append(datos.path("nombre").asText("—"));
        sb.append("\n\nResponde *1* para confirmar o *2* para empezar de nuevo.");
        return sb.toString();
    }

    // =================================================================
    // Responder
    // =================================================================

    private String aplicar(UUID businessId, WhatsappSession s, String texto) {
        ObjectNode datos = datos(s);
        String paso = s.getStepCode();

        switch (paso == null ? "" : paso) {
            case "SERVIC" -> {
                String elegido = opcion(datos, texto);
                if (elegido == null) return norepetir(businessId, s, paso);
                datos.putArray("offeringIds").add(elegido);
            }
            case "PROFES" -> {
                String elegido = opcion(datos, texto);
                if (elegido == null) return norepetir(businessId, s, paso);
                if (elegido.isEmpty()) datos.putNull("employeeId");
                else datos.put("employeeId", elegido);
            }
            case "FECHOR" -> {
                String elegido = opcion(datos, texto);
                if (elegido == null) return norepetir(businessId, s, paso);
                String[] partes = elegido.split("\\|");
                datos.put("startUtc", partes[0]);
                // El hueco manda quién atiende: con "cualquiera" elegido, es
                // el hueco el que decide, no una segunda resolución.
                if (partes.length > 1) datos.put("employeeId", partes[1]);
            }
            case "DATBAS" -> {
                if (texto.length() < 2) {
                    return texto(businessId, "AGEND_PIDE_NOMBRE",
                            "¿A nombre de quién la reservo?");
                }
                datos.put("nombre", texto.length() > 120 ? texto.substring(0, 120) : texto);
            }
            case "CONFIR" -> {
                if (!"1".equals(texto)) {
                    repo.dropSession(businessId, s.getPhoneE164());
                    return "Listo, no reservé nada. Escríbeme \"hola\" cuando quieras empezar.";
                }
                return reservar(businessId, s, datos);
            }
            default -> {
                return preguntar(businessId, s, primerPaso(businessId));
            }
        }

        s.setDataJson(datos.toString());
        String siguiente = pasoTras(businessId, paso);
        if (siguiente == null) return reservar(businessId, s, datos);
        return preguntar(businessId, s, siguiente);
    }

    /** Lo que no se entiende se vuelve a preguntar; no se adivina. */
    private String norepetir(UUID businessId, WhatsappSession s, String paso) {
        return "No entendí. Responde con el número de la opción.\n\n"
                + preguntar(businessId, s, paso);
    }

    // =================================================================
    // Reservar
    // =================================================================

    private String reservar(UUID businessId, WhatsappSession s, ObjectNode datos) {
        UUID sede = sedePrincipal(businessId);
        List<UUID> servicios = uuids(datos, "offeringIds");
        UUID empleado = uuidOrNull(datos, "employeeId");
        String inicio = datos.path("startUtc").asText("");
        if (sede == null || servicios.isEmpty() || empleado == null || inicio.isEmpty()) {
            repo.dropSession(businessId, s.getPhoneE164());
            return texto(businessId, "AGEND_ERROR",
                    "Me faltan datos. Escríbeme \"hola\" y lo hacemos de nuevo.");
        }

        // El cliente se identifica por SU NÚMERO, que es el que escribió: no
        // hay que preguntarlo y no se puede falsear.
        //
        // Se usa `findOrCreateByPhone` y no un builder a mano: es el único
        // sitio que sabe cómo nace un cliente —normaliza el teléfono a E.164 y
        // pone los contadores a cero, que son columnas NOT NULL—. Construirlo
        // aquí reventaba con "NoShowCount cannot be null", y además quien
        // vuelve a reservar cae en su ficha de siempre con su historial.
        BusinessClient cliente = clients.findOrCreateByPhone(
                businessId, s.getPhoneE164(), datos.path("nombre").asText(null));

        // Verificado por construcción: el mensaje llegó DESDE ese número y
        // quien lo entrega es Meta.
        //
        // Por su propia operación y no con un setter + update: `applyChanges`
        // excluye a propósito las marcas de verificación, así que el update
        // genérico respondía bien y dejaba la columna en NULL.
        cliente = clients.markPhoneVerified(cliente.getId());

        Business negocio = businesses.getById(businessId);
        ZoneId zona = zonaDe(businessId);
        BusinessBookingPolicy politica = policies.forBusiness(businessId);

        Map<UUID, Offering> catalogo = new LinkedHashMap<>();
        offerings.findByBusiness(businessId).forEach(o -> catalogo.put(o.getId(), o));

        Map<UUID, Integer> propias = new LinkedHashMap<>();
        for (EmployeeOffering eo : employeeOfferings.findByOfferingIds(servicios)) {
            if (empleado.equals(eo.getEmployeeId())
                    && eo.getDurationMinutes() != null && eo.getDurationMinutes() > 0) {
                propias.put(eo.getOfferingId(), eo.getDurationMinutes());
            }
        }

        List<BookingCommand.Line> lineas = new ArrayList<>();
        for (UUID id : servicios) {
            Offering o = catalogo.get(id);
            if (o == null) continue;
            lineas.add(new BookingCommand.Line(o.getId(), o.getName(), o.getPrice(),
                    propias.getOrDefault(id, o.getDurationMinutes()), BigDecimal.ZERO));
        }

        try {
            Appointment cita = booking.book(new BookingCommand(
                    businessId, sede, empleado, cliente.getId(),
                    AppointmentChannel.WHATSAPP, null, Instant.parse(inicio), zona, lineas,
                    false, Boolean.TRUE.equals(politica.getRequiresManualConfirmation()),
                    politica.toEnginePolicy(), Instant.now(), "Reservada por WhatsApp", null));

            repo.dropSession(businessId, s.getPhoneE164());
            return conValores(texto(businessId, "AGEND_CONFIRMADA",
                    "Listo. Te esperamos el {{FECHA}} a las {{HORA}}. Tu código es {{CODIGO}}."),
                    Map.of("FECHA", fecha(cita.getStartUtc(), zona),
                           "HORA", cita.getStartUtc().atZone(zona).format(HORA),
                           "CODIGO", cita.getPublicCode(),
                           "NEGOCIO", nombre(negocio)));

        } catch (RuntimeException ex) {
            // Que el hueco se ocupe entre que se ofrece y se confirma es
            // NORMAL, no un error: se vuelve a preguntar la hora con lo que
            // queda libre AHORA.
            log.info("Hueco perdido en WhatsApp para {}: {}", s.getPhoneE164(), ex.getMessage());
            datos.remove("startUtc");
            s.setDataJson(datos.toString());
            return texto(businessId, "AGEND_SLOT_OCUPADO",
                    "Justo se ocupó esa hora. Estas son las que quedan:")
                    + "\n\n" + preguntar(businessId, s, "FECHOR");
        }
    }

    // =================================================================
    // Utilidades
    // =================================================================

    private WhatsappSession nueva(UUID businessId, String phone) {
        repo.dropSession(businessId, phone);
        return WhatsappSession.builder()
                .businessId(businessId)
                .phoneE164(phone)
                .flowCode("AGEND")
                .dataJson("{}")
                .expiresAt(java.time.LocalDateTime.now().plusHours(VIGENCIA_HORAS))
                .build();
    }

    private static boolean esReinicio(String t) {
        String s = t.toLowerCase(ES);
        return s.equals("hola") || s.equals("menu") || s.equals("menú")
                || s.equals("inicio") || s.equals("empezar");
    }

    private ObjectNode datos(WhatsappSession s) {
        try {
            var nodo = json.readTree(s.getDataJson() == null ? "{}" : s.getDataJson());
            return nodo.isObject() ? (ObjectNode) nodo : json.createObjectNode();
        } catch (Exception ex) {
            return json.createObjectNode();
        }
    }

    private void guardarOpciones(ObjectNode datos, List<String> ids) {
        var arr = datos.putArray("opciones");
        ids.forEach(arr::add);
    }

    /** El id que corresponde al número que respondió, o null si no vale. */
    private String opcion(ObjectNode datos, String texto) {
        var arr = datos.path("opciones");
        if (!arr.isArray() || arr.isEmpty()) return null;
        int n;
        try {
            n = Integer.parseInt(texto.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
        if (n < 1 || n > arr.size()) return null;
        return arr.get(n - 1).asText();
    }

    private List<UUID> uuids(ObjectNode datos, String campo) {
        List<UUID> out = new ArrayList<>();
        datos.path(campo).forEach(n -> {
            try {
                out.add(UUID.fromString(n.asText()));
            } catch (IllegalArgumentException ignored) {
                // un id corrupto en la sesión no puede tumbar la conversación
            }
        });
        return out;
    }

    private UUID uuidOrNull(ObjectNode datos, String campo) {
        String v = datos.path(campo).asText(null);
        if (v == null || v.isBlank()) return null;
        try {
            return UUID.fromString(v);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<Employee> empleadosQueHacen(UUID businessId, List<UUID> servicios) {
        UUID sede = sedePrincipal(businessId);
        if (sede == null) return List.of();
        List<Employee> todos = employees.findByBranchId(sede);
        if (servicios.isEmpty()) return todos;

        Set<UUID> capaces = employeeOfferings.findByOfferingIds(servicios).stream()
                .map(EmployeeOffering::getEmployeeId)
                .collect(java.util.stream.Collectors.toSet());
        // Sin asignaciones explícitas, todos pueden: es como se comporta el
        // motor de disponibilidad, y dos criterios distintos ofrecerían gente
        // a la que después no se le puede reservar.
        return capaces.isEmpty() ? todos
                : todos.stream().filter(e -> capaces.contains(e.getId())).toList();
    }

    private Map<String, String> nombresDe(List<Employee> equipo) {
        Set<UUID> personas = equipo.stream().map(Employee::getThirdPartyId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (personas.isEmpty()) return Map.of();
        try {
            return people.personNames(personas);
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private UUID sedePrincipal(UUID businessId) {
        return branches.findByBusinessId(businessId).stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsMain()))
                .map(b -> b.getId())
                .findFirst()
                .orElseGet(() -> branches.findByBusinessId(businessId).stream()
                        .map(b -> b.getId()).findFirst().orElse(null));
    }

    private String nombresServicios(UUID businessId, List<UUID> ids) {
        Map<UUID, String> catalogo = new LinkedHashMap<>();
        offerings.findByBusiness(businessId).forEach(o -> catalogo.put(o.getId(), o.getName()));
        List<String> out = ids.stream().map(catalogo::get)
                .filter(java.util.Objects::nonNull).toList();
        return out.isEmpty() ? "tu servicio" : String.join(" + ", out);
    }

    private ZoneId zonaDe(UUID businessId) {
        try {
            Business b = businesses.getById(businessId);
            return ZoneId.of(b.getTimeZone() == null ? "America/Bogota" : b.getTimeZone());
        } catch (RuntimeException ex) {
            return ZoneId.of("America/Bogota");
        }
    }

    private static String nombre(Business b) {
        return b.getTradeName() != null && !b.getTradeName().isBlank()
                ? b.getTradeName() : b.getName();
    }

    /** "viernes 12 de septiembre a las 09:00". */
    private static String cuando(Instant t, ZoneId zona) {
        var d = t.atZone(zona);
        return d.getDayOfWeek().getDisplayName(TextStyle.FULL, ES) + " "
                + d.getDayOfMonth() + " de " + d.getMonth().getDisplayName(TextStyle.FULL, ES)
                + " a las " + d.format(HORA);
    }

    private static String fecha(Instant t, ZoneId zona) {
        var d = t.atZone(zona);
        return d.getDayOfWeek().getDisplayName(TextStyle.FULL, ES) + " "
                + d.getDayOfMonth() + " de " + d.getMonth().getDisplayName(TextStyle.FULL, ES);
    }

    private static String pesos(BigDecimal v) {
        return v == null ? "" : "$" + v.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** El texto configurado, por código; si no está, el de reserva. */
    private String texto(UUID businessId, String codigo, String porDefecto) {
        try {
            Map<String, String> textos = system.flowMessages().data();
            String t = textos == null ? null : textos.get(codigo);
            return (t == null || t.isBlank()) ? porDefecto : t;
        } catch (RuntimeException ex) {
            return porDefecto;
        }
    }

    private String conNegocio(UUID businessId, String plantilla) {
        return conValores(plantilla, Map.of("NEGOCIO", nombre(businesses.getById(businessId))));
    }

    private static String conValores(String plantilla, Map<String, String> valores) {
        String out = plantilla;
        for (var e : valores.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        // Lo que quede sin valor se quita: un "{{HORAS}}" en pantalla es peor
        // que un hueco.
        return out.replaceAll("\\{\\{\\s*\\w+\\s*\\}\\}", "").trim();
    }
}
