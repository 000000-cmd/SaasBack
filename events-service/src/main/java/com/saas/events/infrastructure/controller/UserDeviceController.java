package com.saas.events.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.events.domain.model.UserDevice;
import com.saas.events.domain.port.out.IUserDeviceRepositoryPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Registro de dispositivos para push. Lo llama la app movil al arrancar, cada
 * vez: el token de FCM se renueva por su cuenta y un token viejo deja de servir
 * en silencio.
 */
@RestController
@RequestMapping("/notification/devices")
@RequiredArgsConstructor
public class UserDeviceController {

    private final IUserDeviceRepositoryPort devices;

    public record RegisterRequest(
            @NotNull UUID thirdPartyId,
            @NotBlank String fcmToken,
            @NotBlank String platform,
            String appVersion) {}

    /**
     * Alta o reasignacion. Si el token ya existe, se REASIGNA al nuevo dueno en
     * vez de crear otra fila: el mismo telefono cambia de manos cuando alguien
     * cierra sesion y entra otro, y si no se reasignara el dueno anterior
     * seguiria recibiendo notificaciones ajenas.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserDevice>> register(@Valid @RequestBody RegisterRequest req) {
        UserDevice device = devices.findByToken(req.fcmToken()).orElse(null);
        if (device == null) {
            device = UserDevice.builder()
                    .thirdPartyId(req.thirdPartyId())
                    .fcmToken(req.fcmToken().trim())
                    .platform(req.platform().toUpperCase())
                    .appVersion(req.appVersion())
                    .lastSeenAt(LocalDateTime.now())
                    .build();
        } else {
            device.setThirdPartyId(req.thirdPartyId());
            device.setPlatform(req.platform().toUpperCase());
            device.setAppVersion(req.appVersion());
            device.setLastSeenAt(LocalDateTime.now());
            device.setEnabled(Boolean.TRUE);
        }
        return ResponseEntity.ok(ApiResponse.success(devices.save(device)));
    }

    /** Baja al cerrar sesion: este telefono deja de recibir lo de esta persona. */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> unregister(@RequestParam String fcmToken) {
        devices.deleteByToken(fcmToken);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "eliminado")));
    }
}
