package com.saas.business.infrastructure.controller;

import com.saas.business.application.service.AttachmentStorageService;
import com.saas.common.dto.ApiResponse;
import com.saas.common.security.IUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Endpoint UNIFICADO de adjuntos. Toda subida de archivo o imagen del sistema
 * pasa por aquí: {@code POST /attachments} con {@code category} (avatar, logo…)
 * y el archivo. El almacenamiento genera un nombre seguro y devuelve la URL.
 *
 * <p>El {@code refId} por defecto es el propio usuario autenticado: así un
 * avatar se nombra por su dueño sin confiar en un parámetro del cliente. Para
 * categorías de negocio (logo/landing) el editor pasa el businessId como refId
 * — endurecer eso contra el token queda pendiente: hoy el principal NO expone
 * businessId (habría que añadir el claim al JWT).</p>
 */
@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentStorageService storage;

    @PostMapping("/attachments")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @RequestParam("category") String category,
            @RequestParam(value = "refId", required = false) String refId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal IUserPrincipal principal) {

        String owner = (refId != null && !refId.isBlank())
                ? refId
                : (principal != null ? principal.getUserId().toString() : null);
        String url = storage.store(file, category, owner);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url), "Adjunto subido"));
    }

    /** Servicio público de los adjuntos (imágenes de avatar, logo, etc.). */
    @GetMapping("/public/attachments/{category}/{filename}")
    public ResponseEntity<Resource> serve(@PathVariable String category, @PathVariable String filename) {
        try {
            Path file = storage.resolve(category, filename);
            if (!Files.exists(file)) return ResponseEntity.notFound().build();
            String contentType = Files.probeContentType(file);
            Resource resource = new UrlResource(file.toUri());
            return ResponseEntity.ok()
                    .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
