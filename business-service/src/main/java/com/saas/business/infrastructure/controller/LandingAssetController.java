package com.saas.business.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Subida de imágenes de la landing (hero/galería/logo). Autenticado: lo usa el
 * editor "Mi página". Los archivos se guardan en disco local (patrón del APK en
 * system-service) y se sirven públicos por {@code GET /public/landing-assets/{file}}.
 */
@Slf4j
@RestController
@RequestMapping("/landing/assets")
public class LandingAssetController {

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED = Set.of("image/png", "image/jpeg", "image/webp", "image/svg+xml");
    private static final Map<String, String> EXT = Map.of(
            "image/png", "png", "image/jpeg", "jpg", "image/webp", "webp", "image/svg+xml", "svg");

    private final Path storageDir;

    public LandingAssetController(@Value("${business.landing.storage-dir:./storage/landing}") String dir) {
        this.storageDir = Path.of(dir).toAbsolutePath().normalize();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(@RequestParam("businessId") UUID businessId,
                                                                   @RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw new BusinessException("Formato no soportado: usa PNG, JPG, WEBP o SVG");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("La imagen supera el máximo de 5MB");
        }
        try {
            Files.createDirectories(storageDir);
            String filename = businessId + "-" + UUID.randomUUID() + "." + EXT.get(contentType);
            Files.copy(file.getInputStream(), storageDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            // URL efectiva vía gateway (context-path /business + prefijo público).
            String url = "/business/public/landing-assets/" + filename;
            log.info("Asset de landing subido: businessId={} file={}", businessId, filename);
            return ResponseEntity.ok(ApiResponse.success(Map.of("url", url), "Imagen subida"));
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar la imagen: " + e.getMessage());
        }
    }
}
