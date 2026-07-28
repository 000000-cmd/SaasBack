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
    private static final Set<String> CATEGORIES = Set.of("avatar", "logo", "landing", "gallery");

    private final Path baseDir;

    public AttachmentStorageService(@Value("${business.attachments.storage-dir:./storage/attachments}") String dir) {
        this.baseDir = Path.of(dir).toAbsolutePath().normalize();
    }

    /**
     * Guarda el archivo y devuelve su URL pública (vía gateway).
     *
     * @param category una de {@link #CATEGORIES}
     * @param refId    a qué pertenece (userId, businessId…); sólo para nombrar, se sanea
     */
    public String store(MultipartFile file, String category, String refId) {
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
            Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            String url = "/business/public/attachments/" + category + "/" + filename;
            log.info("Adjunto guardado: category={} ref={} file={}", category, safeRef, filename);
            return url;
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar el adjunto: " + e.getMessage());
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
