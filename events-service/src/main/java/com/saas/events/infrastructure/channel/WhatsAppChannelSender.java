package com.saas.events.infrastructure.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.events.infrastructure.client.BusinessInternalClient;
import com.saas.events.domain.model.Attachment;
import com.saas.events.domain.model.ChannelType;
import com.saas.events.domain.model.NotificationSetting;
import com.saas.events.domain.model.SendStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envio por la Cloud API de WhatsApp.
 *
 * <h3>Las credenciales NO viven en la base</h3>
 * <p>{@code notification_setting} lo edita un administrador desde una pantalla,
 * y ahi dentro hay una {@code ApiKey} de correo que ya es discutible. Un token
 * de WhatsApp con permiso para escribirle a cualquiera en nombre de la
 * plataforma no puede quedar en una tabla que una pantalla lee. Van por
 * entorno, que es donde vive un secreto de despliegue.</p>
 *
 * <h3>Sin credenciales NO falla: dice que no hay proveedor</h3>
 * <p>Es la diferencia entre "el mensaje se redacto y no salio porque el canal
 * no esta conectado" y "el mensaje se perdio". Lo primero se arregla
 * configurando; lo segundo se investiga durante una tarde.</p>
 *
 * <h3>Por que este bean reclama WHATSAPP siempre</h3>
 * <p>Si se registrara solo con credenciales, WHATSAPP se lo quedaria
 * {@link NoProviderChannelSender} cuando no las hay — y cual de los dos gana
 * dependeria del orden en que Spring inyecte la lista. Un fallo que no se
 * reproduce. Aqui hay exactamente UN bean por canal, y la falta de
 * configuracion es un resultado, no una ausencia.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppChannelSender implements ChannelSender {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final ObjectMapper mapper;
    private final BusinessInternalClient negocios;

    @Value("${saas.whatsapp.api-url:https://graph.facebook.com/v21.0}")
    private String apiUrl;

    /** Id del numero remitente que da Meta. Vacio = canal sin conectar. */
    @Value("${saas.whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${saas.whatsapp.access-token:}")
    private String accessToken;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT).build();

    @Override
    public boolean supports(ChannelType type) {
        return type == ChannelType.WHATSAPP;
    }

    @Override
    public Outcome send(NotificationSetting s, String recipient, String subject,
                        String body, Attachment attachment, UUID businessId) {

        // Primero las del NEGOCIO: es su numero el que quiere ver el cliente, y
        // el unico desde el que puede contestar. Las de la plataforma quedan
        // como respaldo para quien todavia no conecto el suyo.
        String numero = phoneNumberId;
        String token = accessToken;
        if (businessId != null) {
            try {
                BusinessInternalClient.WhatsappCredentials c = negocios.whatsappCredentials(businessId);
                if (c != null && c.phoneNumberId() != null && c.accessToken() != null) {
                    numero = c.phoneNumberId();
                    token = c.accessToken();
                }
            } catch (RuntimeException e) {
                // Que no se pueda preguntar no puede tumbar el aviso: se sigue
                // con el numero de la plataforma y queda dicho por que.
                log.warn("No se pudieron leer las credenciales de WhatsApp del negocio {}: {}",
                        businessId, e.getMessage());
            }
        }

        if (numero == null || numero.isBlank() || token == null || token.isBlank()) {
            return new Outcome(SendStatus.NO_PROVIDER, null,
                    "WhatsApp sin credenciales: el negocio no ha conectado su número "
                            + "y la plataforma tampoco tiene uno configurado");
        }
        if (recipient == null || recipient.isBlank()) {
            return new Outcome(SendStatus.SKIPPED, null, "Destinatario vacío");
        }

        try {
            // El adjunto se ignora a proposito. Por WhatsApp un fichero exige
            // subirlo antes y mandar su id; el cuerpo ya lleva el ENLACE, que
            // es lo que hacen las plantillas de este canal. Preferible que
            // llegue el mensaje sin el papel a que no llegue.
            String json = mapper.writeValueAsString(Map.of(
                    "messaging_product", "whatsapp",
                    "recipient_type", "individual",
                    "to", soloDigitos(recipient),
                    "type", "text",
                    "text", Map.of("preview_url", true, "body", body == null ? "" : body)));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/" + numero + "/messages"))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() / 100 == 2) {
                return new Outcome(SendStatus.SENT, messageIdDe(res.body()), null);
            }
            // El cuerpo del error de Meta dice EXACTAMENTE que pasa ("fuera de
            // la ventana de 24 horas", "plantilla no aprobada"). Guardarlo
            // recortado es lo unico que permite arreglarlo sin adivinar.
            log.warn("WhatsApp respondio {}: {}", res.statusCode(), recortar(res.body()));
            return new Outcome(SendStatus.FAILED, null,
                    "HTTP " + res.statusCode() + ": " + recortar(res.body()));

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new Outcome(SendStatus.FAILED, null, "Envío interrumpido");
        } catch (Exception ex) {
            return new Outcome(SendStatus.FAILED, null, recortar(String.valueOf(ex.getMessage())));
        }
    }

    /** El id que devuelve Meta, para poder cruzarlo con sus acuses. */
    private String messageIdDe(String cuerpo) {
        try {
            var nodo = mapper.readTree(cuerpo).path("messages").path(0).path("id");
            return nodo.isMissingNode() ? null : nodo.asText();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Meta quiere el numero sin "+" ni espacios. Se manda en E.164 y aqui se
     * limpia: normalizarlo en el sitio donde se guarda seria perder el formato
     * que el resto del sistema usa.
     */
    private static String soloDigitos(String telefono) {
        return telefono.replaceAll("[^0-9]", "");
    }

    private static String recortar(String s) {
        if (s == null) return null;
        return s.length() > 400 ? s.substring(0, 400) : s;
    }
}
