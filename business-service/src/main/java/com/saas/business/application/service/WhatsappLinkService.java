package com.saas.business.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.business.domain.model.BusinessBookingPolicy;
import com.saas.business.domain.port.in.IBusinessBookingPolicyUseCase;
import com.saas.business.domain.port.out.IBusinessBookingPolicyRepositoryPort;
import com.saas.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Conectar el WhatsApp de UN negocio.
 *
 * <h3>Que hace falta y por que se pide asi</h3>
 * El dueno pega dos cosas que saca de su cuenta de Meta: el identificador de su
 * numero ({@code phone_number_id}) y un token permanente. No hay QR: escanear
 * un QR es el protocolo de WhatsApp Web, que solo funciona con librerias no
 * oficiales y por el que Meta banea numeros. Esta es la via oficial.
 *
 * <h3>Conectar COMPRUEBA, no guarda a ciegas</h3>
 * Antes de guardar nada se le pregunta a Meta por ese numero con ese token. Si
 * responde, se guarda junto con el numero legible y el nombre verificado que
 * Meta devuelve — y asi el dueno ve en pantalla exactamente lo que veran sus
 * clientes. Guardar sin comprobar dejaria el negocio "conectado" y sin recibir
 * una sola cita, sin ninguna senal de por que.
 *
 * <h3>El token no vuelve a salir de aqui</h3>
 * Se guarda cifrado ({@link SecretBox}) y la pantalla solo recibe si hay o no
 * token, nunca su valor. Un endpoint que devuelve el token para "rellenar el
 * formulario" es la forma mas comun de filtrarlo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappLinkService {

    private final IBusinessBookingPolicyUseCase policies;
    private final IBusinessBookingPolicyRepositoryPort repo;
    private final SecretBox secretos;
    private final ObjectMapper mapper;

    @Value("${saas.whatsapp.api-url:https://graph.facebook.com/v21.0}")
    private String apiUrl;

    /**
     * La URL publica de ESTE servidor, la que se le da a Meta como webhook.
     *
     * <p>No se puede deducir: dentro del contenedor la direccion es otra, y
     * adivinarla acertaria en desarrollo y daria una URL falsa en produccion,
     * que es justo donde importa.</p>
     */
    @Value("${saas.api.public-url:http://localhost:8080}")
    private String apiPublicUrl;

    /** La ruta del webhook. Un solo sitio: la usan Meta, la pantalla y esto. */
    public static final String RUTA_WEBHOOK = "/business/public/whatsapp/webhook";

    public String urlDelWebhook() {
        return apiPublicUrl.replaceAll("/+$", "") + RUTA_WEBHOOK;
    }

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    /**
     * Lo que la pantalla puede saber.
     *
     * <p>El token y el secreto de la app NO salen nunca. El {@code verifyToken}
     * SI: es la palabra que el dueno tiene que pegar en Meta al dar de alta el
     * webhook, y no autoriza nada por si sola.</p>
     */
    public record Estado(boolean conectado, boolean enabled, String phoneNumberId,
                         String wabaId, String displayPhone, String verifiedName,
                         LocalDateTime verifiedAt, String verifyToken,
                         boolean conSecretoDeApp,
                         /** Cuando se dio de alta el webhook solo. Nulo = a mano. */
                         LocalDateTime webhookAt,
                         /** La URL que hay que pegar en Meta si hubo que hacerlo a mano. */
                         String urlWebhook) {}

    public Estado estado(UUID businessId) {
        BusinessBookingPolicy p = policies.forBusiness(businessId);
        boolean conectado = p.getWhatsappPhoneId() != null
                && p.getWhatsappAccessToken() != null;
        return new Estado(conectado, Boolean.TRUE.equals(p.getWhatsappEnabled()),
                p.getWhatsappPhoneId(), p.getWhatsappWabaId(), p.getWhatsappDisplayPhone(),
                p.getWhatsappVerifiedName(), p.getWhatsappVerifiedAt(),
                p.getWhatsappVerifyToken(), p.getWhatsappAppSecret() != null,
                p.getWhatsappWebhookAt(), urlDelWebhook());
    }

    /**
     * Comprueba las credenciales contra Meta y, si sirven, las guarda.
     *
     * @throws BusinessException con lo que respondio Meta. Ese texto dice
     *         EXACTAMENTE que pasa ("token caducado", "numero no encontrado"),
     *         y es lo unico que permite arreglarlo sin adivinar.
     */
    public Estado conectar(UUID businessId, String phoneNumberId, String wabaId,
                           String token, String appSecret) {
        JsonNode datos = preguntarAMeta(phoneNumberId, token);

        BusinessBookingPolicy antes = policies.forBusiness(businessId);

        // La palabra del apreton de manos la genera el sistema: si la eligiera
        // el dueno acabaria siendo "12345", y es lo unico que impide que
        // cualquiera dé de alta un webhook contra este negocio. Se conserva la
        // que ya hubiera, para no invalidar un webhook que ya funciona.
        String verify = antes.getWhatsappVerifyToken() != null
                ? antes.getWhatsappVerifyToken()
                : UUID.randomUUID().toString().replace("-", "");

        // Conectar es querer usarlo: dejarlo apagado obligaria a un segundo
        // interruptor que nadie encuentra.
        repo.saveWhatsappCredentials(businessId, phoneNumberId.trim(),
                wabaId == null || wabaId.isBlank() ? null : wabaId.trim(),
                secretos.cifrar(token.trim()),
                appSecret == null || appSecret.isBlank() ? null : secretos.cifrar(appSecret.trim()),
                verify,
                datos.path("display_phone_number").asText(null),
                datos.path("verified_name").asText(null),
                LocalDateTime.now(), true);

        // Y se intenta dar de alta el webhook SOLO. Si sale, el dueño no vuelve
        // a Meta; si no, se le enseñan las instrucciones a mano. Un fallo aqui
        // no deshace la conexion: enviar ya funciona.
        if (wabaId != null && !wabaId.isBlank()
                && darDeAltaElWebhook(wabaId.trim(), token.trim(), verify)) {
            repo.saveWhatsappWebhook(businessId, LocalDateTime.now());
        }

        Estado e = estado(businessId);
        log.info("WhatsApp conectado para el negocio {} ({}), webhook {}",
                businessId, e.displayPhone(), e.webhookAt() != null ? "automático" : "a mano");
        return e;
    }

    /**
     * Da de alta el webhook contra Meta, sin que el dueño vuelva alli.
     *
     * <p>{@code POST /{WABA_ID}/subscribed_apps} hace las dos cosas de una vez:
     * suscribe la app del negocio a su cuenta de WhatsApp y apunta las entregas
     * a esta URL con nuestra palabra de apreton de manos.</p>
     *
     * <p>Devuelve si se consiguio. NO lanza: este es el paso que el dueño podia
     * hacer a mano, y tumbar una conexion que ya sirve para enviar porque este
     * atajo fallo seria peor que el problema.</p>
     */
    private boolean darDeAltaElWebhook(String wabaId, String token, String verify) {
        try {
            String cuerpo = mapper.writeValueAsString(java.util.Map.of(
                    "override_callback_uri", urlDelWebhook(),
                    "verify_token", verify));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/" + wabaId + "/subscribed_apps"))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 == 2
                    && mapper.readTree(res.body()).path("success").asBoolean(false)) {
                return true;
            }
            log.warn("No se pudo dar de alta el webhook del negocio: {}", motivo(res.body()));
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("No se pudo dar de alta el webhook: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Desconecta y BORRA el token.
     *
     * <p>Dejarlo guardado "por si vuelve" seria conservar un secreto que ya
     * nadie usa, que es la definicion de una fuga esperando.</p>
     */
    public Estado desconectar(UUID businessId) {
        policies.forBusiness(businessId);
        repo.saveWhatsappCredentials(businessId, null, null, null, null, null, null, null,
                                     null, false);
        return estado(businessId);
    }

    /** El token en claro, SOLO para que el servicio de envio pueda mandar. */
    public String tokenDe(UUID businessId) {
        return secretos.descifrar(policies.forBusiness(businessId).getWhatsappAccessToken());
    }

    /** El secreto de la app, para comprobar la firma de lo que llega de Meta. */
    public String appSecretDe(BusinessBookingPolicy p) {
        return p == null ? null : secretos.descifrar(p.getWhatsappAppSecret());
    }

    /** Manda un mensaje de prueba al numero que diga el dueno. */
    public void probar(UUID businessId, String destino) {
        BusinessBookingPolicy p = policies.forBusiness(businessId);
        String token = tokenDe(businessId);
        if (p.getWhatsappPhoneId() == null || token == null) {
            throw new BusinessException("Conecta primero tu número de WhatsApp.");
        }
        enviar(p.getWhatsappPhoneId(), token, destino,
               "Prueba de conexión: tu WhatsApp ya está conectado y puede recibir citas.");
    }

    // -----------------------------------------------------------------
    // Meta
    // -----------------------------------------------------------------

    private JsonNode preguntarAMeta(String phoneNumberId, String token) {
        if (phoneNumberId == null || phoneNumberId.isBlank() || token == null || token.isBlank()) {
            throw new BusinessException("Hacen falta el identificador del número y el token.");
        }
        String url = apiUrl + "/" + phoneNumberId.trim()
                + "?fields=display_phone_number,verified_name";
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + token.trim())
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new BusinessException("Meta rechazó esas credenciales: " + motivo(res.body()));
            }
            return mapper.readTree(res.body());
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Se interrumpió la comprobación con Meta.");
        } catch (Exception e) {
            throw new BusinessException("No se pudo hablar con Meta: " + e.getMessage());
        }
    }

    private void enviar(String phoneNumberId, String token, String destino, String texto) {
        try {
            String json = mapper.writeValueAsString(java.util.Map.of(
                    "messaging_product", "whatsapp",
                    "recipient_type", "individual",
                    "to", destino.replaceAll("\\D", ""),
                    "type", "text",
                    "text", java.util.Map.of("preview_url", false, "body", texto)));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/" + phoneNumberId + "/messages"))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new BusinessException("No se pudo enviar la prueba: " + motivo(res.body()));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Se interrumpió el envío de prueba.");
        } catch (Exception e) {
            throw new BusinessException("No se pudo enviar la prueba: " + e.getMessage());
        }
    }

    /** El texto que da Meta, si viene; si no, el cuerpo recortado. */
    private String motivo(String cuerpo) {
        try {
            JsonNode m = mapper.readTree(cuerpo).path("error").path("message");
            if (!m.isMissingNode() && !m.asText().isBlank()) return m.asText();
        } catch (Exception ignored) {
            // Meta no siempre responde JSON (un 502 del proxy, por ejemplo).
        }
        return cuerpo == null ? "sin detalle" : cuerpo.substring(0, Math.min(200, cuerpo.length()));
    }
}
