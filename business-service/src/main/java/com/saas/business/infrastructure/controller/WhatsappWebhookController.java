package com.saas.business.infrastructure.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.business.application.service.WhatsappLinkService;
import com.saas.business.domain.model.BusinessBookingPolicy;
import com.saas.business.application.service.WhatsappConversationService;
import com.saas.business.domain.port.out.IBusinessBookingPolicyRepositoryPort;
import com.saas.business.domain.port.out.IWhatsappRepositoryPort;
import com.saas.common.events.EventTypes;
import com.saas.common.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El webhook de WhatsApp.
 *
 * <h3>Firma obligatoria</h3>
 * <p>Cada entrega llega con {@code X-Hub-Signature-256}, que es el HMAC del
 * cuerpo con el secreto de la app. Sin comprobarlo, este endpoint —que es
 * publico y no lleva sesion— seria una forma de que cualquiera cree citas en
 * cualquier agenda escribiendo un JSON. Sin secreto configurado se RECHAZA
 * todo: un canal sin firmar apagado es correcto; abierto es un agujero.</p>
 *
 * <h3>Se responde 200 pase lo que pase</h3>
 * <p>Meta reintenta lo que no acaba en 2xx, y un fallo al interpretar un
 * mensaje se convertiria en el mismo mensaje llegando en bucle. El mensaje se
 * guarda ANTES de interpretarlo; si interpretarlo falla, la fila queda con su
 * error y sin marcar, lista para reintentarse a mano.</p>
 *
 * <h3>Deduplicacion</h3>
 * <p>Por el id de mensaje de Meta, con clave unica. Los webhooks se reentregan
 * —lo dice su documentacion— y sin esto una reentrega crearia una segunda
 * cita.</p>
 */
@Slf4j
@RestController
@RequestMapping("/public/whatsapp")
@RequiredArgsConstructor
public class WhatsappWebhookController {

    private final IWhatsappRepositoryPort repo;
    private final IBusinessBookingPolicyRepositoryPort policies;
    private final WhatsappConversationService conversacion;
    private final OutboxPublisher outbox;
    private final ObjectMapper json;
    private final WhatsappLinkService enlaces;

    @Value("${saas.whatsapp.app-secret:}")
    private String appSecret;

    @Value("${saas.whatsapp.verify-token:}")
    private String verifyToken;

    /**
     * El apreton de manos de Meta al dar de alta el webhook.
     *
     * <p>Devuelve el reto TAL CUAL y en texto plano; envolverlo en el
     * {@code ApiResponse} del proyecto haria que Meta lo rechazara.</p>
     */
    @GetMapping(value = "/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        if (!"subscribe".equals(mode) || token == null || !tokenDeAlguien(token)) {
            log.warn("Verificación de webhook rechazada (mode={})", mode);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("no");
        }
        return ResponseEntity.ok(challenge);
    }

    /**
     * Si esa palabra es la de la plataforma o la de ALGUN negocio conectado.
     *
     * <p>Cada negocio tiene su propia app de Meta y da de alta el webhook por su
     * cuenta, asi que cada uno trae su palabra. Comparando solo contra la de la
     * plataforma, ningun negocio podria terminar de dar de alta su webhook.</p>
     *
     * <p>La comparacion es en tiempo constante por lo mismo que la firma: un
     * {@code equals} que se corta en el primer byte distinto deja medir cuanto
     * se acerto.</p>
     */
    private boolean tokenDeAlguien(String token) {
        byte[] dado = token.getBytes(StandardCharsets.UTF_8);
        if (!verifyToken.isBlank()
                && java.security.MessageDigest.isEqual(
                        verifyToken.getBytes(StandardCharsets.UTF_8), dado)) {
            return true;
        }
        return policies.findByWhatsappVerifyToken(token).isPresent();
    }

    /**
     * Los mensajes.
     *
     * <p>El cuerpo entra como {@code String} y no como objeto: la firma se
     * calcula sobre los BYTES EXACTOS que mando Meta, y deserializar y volver a
     * serializar cambia espacios y orden — la firma dejaria de cuadrar siempre.</p>
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> receive(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String firma,
            @RequestBody(required = false) String cuerpo) {

        // Se resuelve el negocio ANTES de comprobar la firma, porque la firma
        // depende de EL: cada negocio tiene su app de Meta y su secreto. Aqui
        // solo se LEE el cuerpo para saber a quien preguntarle; no se actua
        // sobre el hasta que la firma cuadra.
        BusinessBookingPolicy duena = politicaDeLaEntrega(cuerpo);
        String secreto = enlaces.appSecretDe(duena);
        if (secreto == null) secreto = appSecret;

        if (!firmaValida(secreto, firma, cuerpo)) {
            // 403 y no 200: aqui SI interesa que Meta no lo de por entregado,
            // porque no fue Meta quien lo mando.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("firma inválida");
        }

        try {
            procesar(cuerpo);
        } catch (Exception ex) {
            // Nunca hacia arriba: un 500 convierte el mensaje en un bucle de
            // reintentos de Meta.
            log.error("Webhook de WhatsApp no procesado", ex);
        }
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    // ----------------------------------------------------------- privados

    /**
     * HMAC-SHA256 del cuerpo con el secreto de la app.
     *
     * <p>La comparacion es en tiempo CONSTANTE: un {@code equals} normal se
     * corta en el primer byte distinto, y eso deja medir cuantos bytes se
     * acertaron. Con suficientes intentos se reconstruye la firma.</p>
     */
    private boolean firmaValida(String secreto, String cabecera, String cuerpo) {
        if (secreto == null || secreto.isBlank()) {
            log.warn("Webhook de WhatsApp sin secreto configurado: se rechaza todo");
            return false;
        }
        if (cabecera == null || !cabecera.startsWith("sha256=") || cuerpo == null) return false;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] calculada = mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(calculada.length * 2);
            for (byte b : calculada) hex.append(String.format("%02x", b));

            return java.security.MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    cabecera.substring("sha256=".length()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.warn("No se pudo comprobar la firma: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * El negocio al que va dirigida la entrega, por el numero de destino.
     *
     * <p>Meta lo trae en {@code entry[].changes[].value.metadata.phone_number_id}.
     * Devuelve null si no se reconoce, y entonces se comprueba contra el secreto
     * de la plataforma — que es el caso del numero propio.</p>
     */
    private BusinessBookingPolicy politicaDeLaEntrega(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) return null;
        try {
            String phoneId = json.readTree(cuerpo)
                    .path("entry").path(0).path("changes").path(0)
                    .path("value").path("metadata").path("phone_number_id").asText(null);
            return phoneId == null ? null
                    : policies.findByWhatsappPhoneId(phoneId).orElse(null);
        } catch (Exception ex) {
            // Cuerpo ilegible: lo rechazara la firma de todos modos.
            return null;
        }
    }

    private void procesar(String cuerpo) throws Exception {
        JsonNode raiz = json.readTree(cuerpo);

        for (JsonNode entry : raiz.path("entry")) {
            for (JsonNode cambio : entry.path("changes")) {
                JsonNode valor = cambio.path("value");
                String phoneId = valor.path("metadata").path("phone_number_id").asText(null);
                UUID businessId = negocioDe(phoneId);

                for (JsonNode m : valor.path("messages")) {
                    atender(m, valor, phoneId, businessId, cuerpo);
                }
            }
        }
    }

    private void atender(JsonNode m, JsonNode valor, String phoneId, UUID businessId, String crudo) {
        String waId = m.path("id").asText(null);
        String de = m.path("from").asText(null);
        if (waId == null || de == null) return;

        String texto = m.path("text").path("body").asText(
                // Las respuestas de botones y listas no vienen en `text`. Se
                // aceptan igual: su id es lo que el bot espera.
                m.path("interactive").path("list_reply").path("id").asText(
                        m.path("interactive").path("button_reply").path("id").asText("")));

        // El numero llega sin "+"; dentro del sistema todo va en E.164.
        String telefono = de.startsWith("+") ? de : "+" + de;

        if (!repo.saveIfFirst(waId, businessId, telefono, phoneId, texto, crudo)) {
            // Reentrega: ya se contesto. Volver a contestar seria mandar el
            // mismo mensaje dos veces y, en el paso de confirmar, reservar dos.
            log.debug("Mensaje {} ya visto: reentrega de Meta", waId);
            return;
        }

        if (businessId == null) {
            repo.markProcessed(waId, "No hay negocio para el número " + phoneId);
            return;
        }

        try {
            String respuesta = conversacion.responder(businessId, telefono, texto);
            enviar(businessId, telefono, respuesta);
            repo.markProcessed(waId, null);
        } catch (RuntimeException ex) {
            // La fila queda SIN marcar y con su motivo: se ve y se puede
            // reintentar. Marcarla como hecha perderia el mensaje del cliente.
            repo.markProcessed(waId, recortar(ex.getMessage()));
            log.warn("No se pudo contestar a {}: {}", telefono, ex.getMessage());
        }
    }

    /**
     * La respuesta sale por el MISMO camino que el resto de avisos: al outbox,
     * y de ahi a events-service. Llamar a Meta desde aqui metería una llamada
     * de red en la transacción que acaba de crear la cita.
     */
    private void enviar(UUID businessId, String telefono, String texto) {
        if (texto == null || texto.isBlank()) return;
        outbox.publish(EventTypes.NOTIFICATION_REQUESTED, businessId,
                "whatsapp_reply", UUID.randomUUID(),
                Map.of("notificationCode", "WHATSAPP_REPLY",
                       "to", List.of(telefono),
                       "channels", List.of("WHATSAPP"),
                       "data", Map.of("MENSAJE", texto)));
    }

    /** El negocio dueño del número al que escribieron. */
    private UUID negocioDe(String phoneId) {
        if (phoneId == null || phoneId.isBlank()) return null;
        return policies.findByWhatsappPhoneId(phoneId)
                .map(p -> p.getBusinessId())
                .orElse(null);
    }

    private static String recortar(String s) {
        if (s == null) return "error sin mensaje";
        return s.length() > 480 ? s.substring(0, 480) : s;
    }
}
