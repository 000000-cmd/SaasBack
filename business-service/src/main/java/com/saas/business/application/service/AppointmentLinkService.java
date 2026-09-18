package com.saas.business.application.service;

import com.saas.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * El enlace opaco a UNA cita.
 *
 * <h3>Por que existe</h3>
 * Para ver su cita, el cliente tenia que teclear el codigo publico y los cuatro
 * ultimos digitos de su telefono, y esos cuatro digitos viajaban en el
 * {@code query string}. Un query string se queda escrito en el registro de
 * accesos del servidor, en el historial del navegador y en cualquier proxy por
 * el que pase: es el sitio donde JAMAS debe ir un dato de una persona.
 *
 * <p>Con esto, el aviso que se le manda al cliente lleva un enlace cuyo token
 * ES la credencial: no hace falta que escriba nada, y por la URL no se filtra
 * ni su telefono ni el codigo de la cita.</p>
 *
 * <h3>Como esta hecho</h3>
 * Firmado, no guardado. El token lleva dentro la cita y su fecha de caducidad,
 * y una firma HMAC-SHA256 que impide fabricarlo. No hay tabla de tokens que
 * mantener, ni fila que limpiar, y revocarlos todos es cambiar el secreto.
 *
 * <p>La comparacion de la firma es en tiempo constante ({@code MessageDigest
 * .isEqual}): comparar con {@code equals} filtra, por lo que tarda, cuantos
 * bytes del principio acerto quien lo intenta.</p>
 */
@Slf4j
@Service
public class AppointmentLinkService {

    /** Se firma con esto. Sin secreto configurado, no se emiten enlaces. */
    @Value("${saas.booking.link-secret:}")
    private String secret;

    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    /** Bytes de firma que se conservan. 16 son 128 bits: de sobra, y acorta la URL. */
    private static final int BYTES_FIRMA = 16;

    public boolean configurado() {
        return secret != null && !secret.isBlank();
    }

    /**
     * El token de una cita, valido hasta {@code caduca}.
     *
     * <p>Devuelve {@code null} si no hay secreto: es preferible un aviso sin
     * enlace que un enlace que cualquiera pueda fabricar.</p>
     */
    public String emitir(UUID appointmentId, Instant caduca) {
        if (!configurado()) {
            log.warn("Sin saas.booking.link-secret: no se emiten enlaces a la cita");
            return null;
        }
        String cuerpo = appointmentId + "." + caduca.getEpochSecond();
        String datos = ENC.encodeToString(cuerpo.getBytes(StandardCharsets.UTF_8));
        return datos + "." + ENC.encodeToString(firmar(datos));
    }

    /**
     * La cita que hay dentro del token, si la firma cuadra y no ha caducado.
     *
     * <p>Todos los fallos —firma mala, formato raro, caducado— responden lo
     * MISMO. Distinguirlos le diria a quien lo intenta si acerto la parte de la
     * cita y fallo la firma, que es justo la pista que no debe tener.</p>
     */
    public UUID leer(String token) {
        if (!configurado()) throw invalido();
        if (token == null) throw invalido();

        int punto = token.lastIndexOf('.');
        if (punto <= 0) throw invalido();

        String datos = token.substring(0, punto);
        byte[] firma;
        byte[] cuerpo;
        try {
            firma = DEC.decode(token.substring(punto + 1));
            cuerpo = DEC.decode(datos);
        } catch (IllegalArgumentException e) {
            throw invalido();
        }
        if (!MessageDigest.isEqual(firma, firmar(datos))) throw invalido();

        String[] partes = new String(cuerpo, StandardCharsets.UTF_8).split("\\.");
        if (partes.length != 2) throw invalido();
        try {
            if (Instant.now().getEpochSecond() > Long.parseLong(partes[1])) throw invalido();
            return UUID.fromString(partes[0]);
        } catch (IllegalArgumentException e) {   // cubre tambien NumberFormatException
            throw invalido();
        }
    }

    private byte[] firmar(String datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] completa = mac.doFinal(datos.getBytes(StandardCharsets.UTF_8));
            byte[] corta = new byte[BYTES_FIRMA];
            System.arraycopy(completa, 0, corta, 0, BYTES_FIRMA);
            return corta;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el enlace de la cita", e);
        }
    }

    private BusinessException invalido() {
        return new BusinessException("Ese enlace ya no sirve. Consulta tu cita con el código.");
    }
}
