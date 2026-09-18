package com.saas.business.application.service;

import com.saas.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Guarda un secreto de forma que un volcado de la base no lo entregue.
 *
 * <h3>Por que hace falta</h3>
 * El token de WhatsApp de un negocio permite enviar mensajes EN SU NOMBRE. En
 * claro, quien consiga una copia de la base los tiene todos, de todos los
 * negocios conectados, y sin dejar rastro en ningun sitio.
 *
 * <h3>Por que AES-GCM y no "algo cifrado"</h3>
 * GCM autentica ademas de cifrar: si alguien cambia un byte del texto guardado,
 * el descifrado FALLA en vez de devolver basura que luego se manda a Meta como
 * si fuera un token. Con AES-CBC a secas eso no pasa.
 *
 * <p>La llave vive en la configuracion del servicio, NO en la base. Cifrar
 * guardando la llave al lado no protege de nada — es justo el escenario del que
 * esto defiende.</p>
 *
 * <p>Cada valor lleva su propio IV aleatorio delante. Reutilizar el IV en GCM
 * no es una pega teorica: con dos mensajes bajo el mismo IV se recupera la
 * clave de autenticacion.</p>
 */
@Slf4j
@Service
public class SecretBox {

    /** Frase de la que sale la llave. Vacia = no se puede guardar ningun secreto. */
    @Value("${saas.secrets.key:}")
    private String passphrase;

    private static final int IV_BYTES = 12;      // el tamano que recomienda GCM
    private static final int TAG_BITS = 128;
    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder ENC = Base64.getEncoder();
    private static final Base64.Decoder DEC = Base64.getDecoder();

    public boolean configurado() {
        return passphrase != null && !passphrase.isBlank();
    }

    /** Devuelve {@code null} si entra {@code null}: no se cifra la ausencia. */
    public String cifrar(String claro) {
        if (claro == null) return null;
        exigirLlave();
        try {
            byte[] iv = new byte[IV_BYTES];
            RNG.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, llave(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = c.doFinal(claro.getBytes(StandardCharsets.UTF_8));

            byte[] todo = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, todo, 0, iv.length);
            System.arraycopy(cifrado, 0, todo, iv.length, cifrado.length);
            return ENC.encodeToString(todo);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar el secreto", e);
        }
    }

    public String descifrar(String guardado) {
        if (guardado == null || guardado.isBlank()) return null;
        exigirLlave();
        try {
            byte[] todo = DEC.decode(guardado);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(todo, 0, iv, 0, IV_BYTES);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, llave(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(c.doFinal(todo, IV_BYTES, todo.length - IV_BYTES),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Pasa cuando se cambio la llave: el secreto viejo ya no se puede
            // leer. Se dice claro, porque el sintoma seria "WhatsApp dejo de
            // funcionar solo" y se buscaria en el sitio equivocado.
            log.warn("No se pudo descifrar un secreto guardado: ¿cambió saas.secrets.key?");
            return null;
        }
    }

    /** La llave: SHA-256 de la frase, que da los 32 bytes que pide AES-256. */
    private SecretKeySpec llave() throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest(passphrase.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(bytes, "AES");
    }

    private void exigirLlave() {
        if (!configurado()) {
            throw new BusinessException(
                    "Falta configurar saas.secrets.key: sin esa llave no se pueden "
                            + "guardar credenciales de WhatsApp.");
        }
    }
}
