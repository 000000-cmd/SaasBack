package com.saas.events.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envio a Firebase Cloud Messaging (API HTTP v1).
 *
 * El mensaje lleva bloque {@code notification} y NO solo {@code data}: es lo que
 * hace que Android pinte la notificacion en la bandeja del sistema por su
 * cuenta, incluso con la app CERRADA. Con solo {@code data} la app tendria que
 * estar viva para construirla, que es justo lo que no se quiere.
 *
 * Ademas va un bloque {@code data} con el codigo de la notificacion: es lo que
 * permite que al tocarla la app abra la pantalla que toca en vez de la portada.
 *
 * La credencial la resuelve {@link FcmTokenSource}, que renueva el token solo.
 * Mientras no exista, {@link #isConfigured()} devuelve false y el canal registra
 * NO_PROVIDER en vez de fallar: la bandeja se escribe igual, la notificacion
 * existe y se ve en la web y en el movil — simplemente no suena.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmClient {

    /**
     * Canal de Android. Tiene que existir con este MISMO id en la app; si no
     * coincide, Android entrega la notificacion en el canal por defecto y el
     * usuario no puede silenciarla por separado.
     */
    private static final String CANAL_ANDROID = "moda_erp_avisos";

    private final FcmTokenSource credentials;
    private final RestClient http = RestClient.builder().build();

    public boolean isConfigured() {
        return credentials.isConfigured();
    }

    /**
     * @param tokenInvalid true cuando FCM dice que el token ya no sirve
     *        (UNREGISTERED o INVALID_ARGUMENT). Quien llama debe borrarlo.
     */
    public record PushResult(boolean ok, String messageId, String error, boolean tokenInvalid) {}

    public PushResult send(String deviceToken, String title, String body) {
        return send(deviceToken, title, body, Map.of());
    }

    public PushResult send(String deviceToken, String title, String body, Map<String, String> data) {
        if (!isConfigured()) {
            return new PushResult(false, null, "FCM sin credenciales", false);
        }
        String access = credentials.token();
        if (access == null) {
            return new PushResult(false, null, "No se pudo renovar el token de FCM", false);
        }

        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", title == null || title.isBlank() ? "Notificación" : title);
        notification.put("body", plano(body));

        Map<String, Object> android = new LinkedHashMap<>();
        // Prioridad alta: sin esto Android puede retrasar la entrega cuando el
        // telefono esta en reposo, que es cuando mas importa que llegue.
        android.put("priority", "HIGH");
        android.put("notification", Map.of(
                "channel_id", CANAL_ANDROID,
                // Al tocarla se abre la app en su punto de entrada normal; la
                // ruta concreta la decide el cliente con el bloque `data`.
                "click_action", "FLUTTER_NOTIFICATION_CLICK"));

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("token", deviceToken);
        message.put("notification", notification);
        message.put("android", android);
        if (data != null && !data.isEmpty()) message.put("data", data);

        try {
            var response = http.post()
                    .uri("https://fcm.googleapis.com/v1/projects/" + credentials.projectId() + "/messages:send")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", message))
                    .retrieve()
                    .toEntity(JsonNode.class);
            JsonNode json = response.getBody();
            String id = json != null && json.hasNonNull("name") ? json.get("name").asText() : null;
            return new PushResult(true, id, null, false);

        } catch (org.springframework.web.client.RestClientResponseException ex) {
            String detail = ex.getResponseBodyAsString();
            boolean invalido = detail.contains("UNREGISTERED") || detail.contains("INVALID_ARGUMENT");
            log.warn("FCM rechazo el envio: {} {}", ex.getStatusCode(), detail);
            return new PushResult(false, null, recorta(ex.getStatusCode() + " " + detail), invalido);
        } catch (Exception ex) {
            log.error("Fallo llamando a FCM", ex);
            return new PushResult(false, null, recorta(ex.getMessage()), false);
        }
    }

    /**
     * La bandeja del sistema no pinta HTML: si una plantilla de correo se
     * reutiliza en push, sin esto se veria el marcado en crudo en el telefono.
     */
    private static String plano(String body) {
        if (body == null) return "";
        String s = body.replaceAll("(?is)<(script|style).*?</\\1>", " ")
                       .replaceAll("(?s)<[^>]+>", " ")
                       .replace("&nbsp;", " ").replace("&amp;", "&")
                       .replace("&lt;", "<").replace("&gt;", ">")
                       .replaceAll("\\s+", " ")
                       .trim();
        return s.length() <= 240 ? s : s.substring(0, 237) + "...";
    }

    /** La columna Error es VARCHAR(500). */
    private static String recorta(String s) {
        if (s == null) return null;
        return s.length() <= 500 ? s : s.substring(0, 497) + "...";
    }
}
