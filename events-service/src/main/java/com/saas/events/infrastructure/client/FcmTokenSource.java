package com.saas.events.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * El token de acceso con el que se le habla a FCM.
 *
 * <h3>Por qué existe este archivo</h3>
 * Antes la credencial era una propiedad, {@code saas.fcm.access-token}, con un
 * token pegado a mano. Un token de Google dura UNA HORA. Es decir: aunque
 * alguien lo hubiera configurado, las push habrían dejado de salir sesenta
 * minutos después y el registro habría dicho 401 para siempre. Esa configuración
 * no podía funcionar más que un rato.
 *
 * Lo correcto es lo que hace cualquier cliente de Google: se firma un JWT con la
 * clave privada de la cuenta de servicio, se cambia por un token en
 * {@code oauth2.googleapis.com} y se renueva solo antes de que venza.
 *
 * <h3>Por qué a mano y no con la librería de Google</h3>
 * {@code google-auth-library} arrastra gRPC, Guava y protobuf: decenas de MB en
 * una imagen que corre con 512 MB de heap, para hacer un JWT y un POST. jjwt ya
 * está en el proyecto (es con lo que se firman los tokens de sesión) y hace la
 * parte difícil, que es la firma RS256.
 *
 * <h3>Qué hay que configurar</h3>
 * Una sola cosa: el JSON de la cuenta de servicio de Firebase, en
 * {@code saas.fcm.credentials-path} (ruta a un fichero) o en
 * {@code SAAS_FCM_CREDENTIALS} (el JSON entero como variable de entorno, que es
 * lo cómodo en contenedor). El {@code project-id} sale del propio JSON, así que
 * no hay que repetirlo.
 *
 * Sin credencial, {@link #isConfigured()} devuelve false y el canal de push
 * registra NO_PROVIDER en vez de fallar: la bandeja se escribe igual y la
 * notificación existe, simplemente no hace vibrar el teléfono.
 */
@Slf4j
@Component
public class FcmTokenSource {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    /** Se renueva cinco minutos antes de vencer: nunca se usa uno recién caducado. */
    private static final long MARGEN_SEGUNDOS = 300;

    private final ObjectMapper json = new ObjectMapper();
    private final RestClient http = RestClient.builder().build();

    private final String clientEmail;
    private final PrivateKey privateKey;
    private final String projectId;
    /** Compatibilidad: si alguien todavía pega un token a mano, se respeta. */
    private final String tokenFijo;

    private volatile String cache;
    private volatile Instant caduca = Instant.EPOCH;

    public FcmTokenSource(@Value("${saas.fcm.credentials-path:}") String credentialsPath,
                          @Value("${saas.fcm.credentials:}") String credentialsInline,
                          @Value("${saas.fcm.project-id:}") String projectIdProp,
                          @Value("${saas.fcm.access-token:}") String accessTokenProp) {
        this.tokenFijo = accessTokenProp == null ? "" : accessTokenProp.trim();

        JsonNode cuenta = leerCuenta(credentialsPath, credentialsInline);
        if (cuenta == null) {
            this.clientEmail = null;
            this.privateKey = null;
            this.projectId = projectIdProp == null ? "" : projectIdProp.trim();
            if (!this.tokenFijo.isBlank()) {
                log.warn("FCM configurado con un token fijo. Caduca en una hora: "
                        + "pon el JSON de la cuenta de servicio en saas.fcm.credentials-path.");
            }
            return;
        }

        this.clientEmail = texto(cuenta, "client_email");
        this.projectId = !projectIdProp.isBlank() ? projectIdProp.trim() : texto(cuenta, "project_id");
        this.privateKey = leerClave(texto(cuenta, "private_key"));
        if (this.privateKey != null) {
            log.info("FCM listo para el proyecto '{}' con la cuenta {}", projectId, clientEmail);
        }
    }

    public boolean isConfigured() {
        return !projectId.isBlank()
                && ((clientEmail != null && privateKey != null) || !tokenFijo.isBlank());
    }

    public String projectId() {
        return projectId;
    }

    /** Token vigente, renovándolo si hace falta. {@code null} si no se pudo. */
    public synchronized String token() {
        if (privateKey == null) return tokenFijo.isBlank() ? null : tokenFijo;
        if (cache != null && Instant.now().isBefore(caduca)) return cache;

        try {
            Instant ahora = Instant.now();
            String assertion = Jwts.builder()
                    .issuer(clientEmail)
                    .subject(clientEmail)
                    .audience().add(TOKEN_URI).and()
                    .claim("scope", SCOPE)
                    .issuedAt(Date.from(ahora))
                    .expiration(Date.from(ahora.plusSeconds(3600)))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            form.add("assertion", assertion);

            JsonNode r = http.post().uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);

            if (r == null || !r.hasNonNull("access_token")) {
                log.error("Google no devolvió token de acceso para FCM: {}", r);
                return null;
            }
            cache = r.get("access_token").asText();
            long vive = r.path("expires_in").asLong(3600);
            caduca = Instant.now().plusSeconds(Math.max(60, vive - MARGEN_SEGUNDOS));
            return cache;

        } catch (Exception ex) {
            log.error("No se pudo obtener el token de FCM", ex);
            return null;
        }
    }

    // -----------------------------------------------------------------
    // Lectura de la credencial
    // -----------------------------------------------------------------

    private JsonNode leerCuenta(String path, String inline) {
        try {
            if (inline != null && !inline.isBlank()) return json.readTree(inline.trim());
            if (path != null && !path.isBlank()) {
                Path p = Path.of(path.trim());
                if (!Files.isReadable(p)) {
                    log.warn("saas.fcm.credentials-path apunta a un fichero que no se puede leer: {}", p);
                    return null;
                }
                return json.readTree(Files.readString(p));
            }
        } catch (Exception ex) {
            log.error("El JSON de la cuenta de servicio de FCM no se pudo leer: {}", ex.getMessage());
        }
        return null;
    }

    private static String texto(JsonNode n, String campo) {
        return n.hasNonNull(campo) ? n.get(campo).asText() : null;
    }

    /**
     * La clave del JSON viene en PEM con los saltos de línea escapados como
     * {@code \n} literales. Si no se deshacen, el decodificador Base64 recibe
     * basura y la firma falla con un mensaje que no dice nada de esto.
     */
    private static PrivateKey leerClave(String pem) {
        if (pem == null || pem.isBlank()) return null;
        try {
            String limpio = pem.replace("\\n", "\n")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(limpio);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception ex) {
            log.error("La clave privada de la cuenta de servicio de FCM no es válida: {}", ex.getMessage());
            return null;
        }
    }

    /** Estado legible para el panel de diagnóstico del subsistema. */
    public Map<String, Object> status() {
        return Map.of(
                "configured", isConfigured(),
                "projectId", projectId == null ? "" : projectId,
                "mode", privateKey != null ? "service-account"
                        : (!tokenFijo.isBlank() ? "static-token" : "none"));
    }
}
