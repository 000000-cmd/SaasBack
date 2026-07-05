package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.response.LatestAppResponse;
import com.saas.system.domain.model.AppVersion;
import com.saas.system.domain.port.in.IAppVersionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Distribución PÚBLICA del APK (sin JWT; whitelisted en el gateway).
 *
 * <ul>
 *   <li>{@code GET /system/public/app-versions/latest} — metadata de la vigente
 *       (el APK la usa para exigir actualización y verificar el checksum).</li>
 *   <li>{@code GET /system/public/app-versions/latest/download} — link ESTABLE
 *       y compartible: siempre sirve la vigente (el dueño comparte este).</li>
 *   <li>{@code GET /system/public/app-versions/{id}/download} — histórico.</li>
 * </ul>
 */
@RestController
@RequestMapping("/public/app-versions")
@RequiredArgsConstructor
public class PublicAppController {

    private static final MediaType APK = MediaType.parseMediaType("application/vnd.android.package-archive");

    private final IAppVersionUseCase useCase;

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<LatestAppResponse>> latest() {
        return useCase.findCurrent()
                .map(v -> ResponseEntity.ok(ApiResponse.success(new LatestAppResponse(
                        v.getVersion(), v.getVersionCode(), v.getNotes(),
                        v.getChecksum(), v.getSizeBytes(),
                        "/system/public/app-versions/latest/download"))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("No hay version publicada", 404)));
    }

    @GetMapping("/latest/download")
    public ResponseEntity<Resource> downloadLatest() {
        return useCase.findCurrent()
                .map(this::file)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadById(@PathVariable UUID id) {
        return file(useCase.getById(id));
    }

    private ResponseEntity<Resource> file(AppVersion v) {
        FileSystemResource resource = new FileSystemResource(useCase.binaryPath(v));
        return ResponseEntity.ok()
                .contentType(APK)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + v.getFileName() + "\"")
                .contentLength(v.getSizeBytes())
                .body(resource);
    }
}
