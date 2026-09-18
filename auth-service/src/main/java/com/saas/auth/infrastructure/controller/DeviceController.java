package com.saas.auth.infrastructure.controller;

import com.saas.auth.application.service.DeviceLinkService;
import com.saas.auth.domain.model.UserDeviceLink;
import com.saas.common.dto.ApiResponse;
import com.saas.common.security.IUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "Mis dispositivos": desde donde esta entrando esta cuenta y como cortar uno.
 *
 * <p>El context-path del servicio es {@code /auth}, asi que la ruta publica es
 * {@code /auth/devices}.</p>
 *
 * <p>Solo se opera sobre los aparatos de UNO MISMO: el id del usuario sale del
 * token, nunca de la URL. Sin eso, un identificador cambiado a mano dejaria a
 * cualquiera cerrar la sesion de otro.</p>
 */
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceLinkService devices;

    public record DeviceView(UUID id, String deviceName, String platform,
                             String appVersion, String lastSeenAt, boolean current) {}

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceView>>> mine(
            @AuthenticationPrincipal IUserPrincipal principal) {
        List<DeviceView> out = devices.active(principal.getUserId()).stream()
                .map(DeviceController::toView)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(out));
    }

    @DeleteMapping("/{linkId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> revoke(
            @AuthenticationPrincipal IUserPrincipal principal,
            @PathVariable UUID linkId) {
        // Se comprueba que el vinculo sea suyo mirando su propia lista: es una
        // consulta ya indexada y evita exponer un "existe / no existe" ajeno.
        boolean propio = devices.active(principal.getUserId()).stream()
                .anyMatch(l -> l.getId().equals(linkId));
        if (!propio) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("revoked", false),
                    "Ese dispositivo no está vinculado a tu cuenta"));
        }
        boolean ok = devices.revoke(linkId, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("revoked", ok),
                ok ? "Dispositivo desvinculado" : "Ya estaba desvinculado"));
    }

    private static DeviceView toView(UserDeviceLink l) {
        return new DeviceView(l.getId(), l.getDeviceName(), l.getPlatform(),
                l.getAppVersion(), String.valueOf(l.getLastSeenAt()), false);
    }
}
