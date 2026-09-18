package com.saas.business.application.service;

import com.saas.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Almacenamiento unificado de adjuntos. UN solo sitio para guardar archivos e
 * imágenes, con estructura clara por categoría (subcarpeta) y nombre de archivo
 * SIEMPRE generado ({@code <refId>-<uuid>.<ext>}): el nombre del cliente se
 * descarta por completo, así que espacios, tildes, {@code ../} o cualquier
 * carácter raro no llegan nunca al disco. La extensión sale del content-type
 * validado, no del nombre subido.
 *
 * <p>Las categorías son una lista blanca: separan qué adjunto pertenece a qué
 * (avatar, logo, landing…) y evitan que un {@code category} arbitrario cree
 * carpetas fuera de control.</p>
 */
@Slf4j
@Service
public class AttachmentStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> IMAGE_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp", "image/svg+xml");
    private static final Map<String, String> EXT = Map.of(
            "image/png", "png", "image/jpeg", "jpg", "image/webp", "webp", "image/svg+xml", "svg");

    /** Categorías permitidas y si aceptan sólo imágenes (hoy todas). */
    // receipt: comprobante de un pago electronico de un servicio prestado.
    // result:  foto del trabajo terminado (el corte, las unas...).
    // payroll: comprobante de la CONSIGNACION de nomina al colaborador.
    // Van separadas a proposito: "receipt" prueba que el cliente pago un
    // servicio, "payroll" prueba que la empresa le pago al empleado y "result"
    // es la foto del trabajo. Mezclarlas en "gallery" haria imposible
    // distinguirlas justo cuando alguien reclama un pago.
    private static final Set<String> CATEGORIES =
            Set.of("avatar", "logo", "landing", "gallery", "receipt", "result", "payroll");

    private final Path baseDir;

    public AttachmentStorageService(@Value("${business.attachments.storage-dir:./storage/attachments}") String dir) {
        this.baseDir = Path.of(dir).toAbsolutePath().normalize();
    }

    /**
     * Lo que queda de un adjunto guardado: dónde está y qué es.
     *
     * <p>El {@code hash} es el SHA-256 del CONTENIDO. La URL no sirve para
     * saber si dos adjuntos son el mismo fichero —cada subida genera un nombre
     * nuevo—, y en un comprobante de pago eso importa: el mismo soporte subido
     * a dos pagos distintos no lo notaba nadie.</p>
     */
    public record Stored(String url, String hash) {}

    /**
     * Guarda el archivo y devuelve su URL pública (vía gateway).
     *
     * @param category una de {@link #CATEGORIES}
     * @param refId    a qué pertenece (userId, businessId…); sólo para nombrar, se sanea
     */
    public String store(MultipartFile file, String category, String refId) {
        return storeWithHash(file, category, refId).url();
    }

    /** Igual, pero devolviendo además la huella del contenido. */
    public Stored storeWithHash(MultipartFile file, String category, String refId) {
        if (!CATEGORIES.contains(category)) {
            throw new BusinessException("Categoría de adjunto no soportada: " + category);
        }
        String contentType = file.getContentType();
        if (contentType == null || !IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException("Formato no soportado: usa PNG, JPG, WEBP o SVG");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("El archivo supera el máximo de 5MB");
        }
        // refId sólo alimenta el prefijo del nombre; se limita a [a-z0-9-] para que
        // no pueda inyectar rutas ni caracteres de sistema de archivos.
        String cleaned = (refId == null ? "" : refId).toLowerCase().replaceAll("[^a-z0-9-]", "");
        String safeRef = cleaned.isBlank() ? "x" : cleaned.substring(0, Math.min(36, cleaned.length()));

        try {
            Path dir = baseDir.resolve(category);
            Files.createDirectories(dir);
            String filename = safeRef + "-" + UUID.randomUUID() + "." + EXT.get(contentType);

            // Se leen los bytes UNA vez: sirven para escribir y para la huella.
            // Abrir el stream dos veces no siempre funciona con un multipart, y
            // cuando falla lo hace guardando un fichero vacío.
            byte[] bytes = file.getBytes();
            Files.write(dir.resolve(filename), bytes);

            String url = "/business/public/attachments/" + category + "/" + filename;
            log.info("Adjunto guardado: category={} ref={} file={}", category, safeRef, filename);
            return new Stored(url, sha256(bytes));
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar el adjunto: " + e.getMessage());
        }
    }

    /** SHA-256 en hexadecimal. Es la identidad del contenido, no del fichero. */
    private static String sha256(byte[] bytes) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 lo trae toda JVM; si no estuviera, el problema es otro.
            throw new IllegalStateException("Sin SHA-256 en esta JVM", e);
        }
    }

    /** Resuelve un archivo servible dentro de la categoría, con guarda anti-traversal. */
    public Path resolve(String category, String filename) {
        if (!CATEGORIES.contains(category)
                || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BusinessException("Ruta de adjunto inválida");
        }
        return baseDir.resolve(category).resolve(filename).normalize();
    }
}
