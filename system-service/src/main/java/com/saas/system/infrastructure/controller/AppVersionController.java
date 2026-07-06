package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.common.exception.BusinessException;
import com.saas.system.application.dto.response.AppVersionResponse;
import com.saas.system.application.mapper.AppVersionMapper;
import com.saas.system.domain.port.in.IAppVersionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Administración de versiones del APK (solo ADMIN del sistema): subir el
 * binario, publicarlo como vigente y llevar el histórico. La descarga pública
 * vive en {@link PublicAppController}.
 */
@RestController
@RequestMapping("/app-versions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AppVersionController {

    private final IAppVersionUseCase useCase;
    private final AppVersionMapper mapper;

    /** Histórico (más recientes primero por versionCode). */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppVersionResponse>>> list() {
        List<AppVersionResponse> items = useCase.getAll().stream()
                .sorted(Comparator.comparing(v -> -v.getVersionCode()))
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    /** Sube el APK; con {@code publish=true} queda vigente de una vez. */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<AppVersionResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String version,
            @RequestParam Integer versionCode,
            @RequestParam(required = false) String notes,
            @RequestParam(defaultValue = "true") boolean publish) {
        if (file.isEmpty()) throw new BusinessException("Adjunta el archivo del APK");
        String name = file.getOriginalFilename();
        if (name != null && !name.toLowerCase().endsWith(".apk")) {
            throw new BusinessException("El archivo debe ser un .apk");
        }
        try {
            var created = useCase.upload(version, versionCode, notes, publish, file.getInputStream());
            return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<AppVersionResponse>> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponse(useCase.publish(id)), "Version publicada"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Version eliminada del historico"));
    }
}
