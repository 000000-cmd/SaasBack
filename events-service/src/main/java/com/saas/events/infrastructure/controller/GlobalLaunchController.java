package com.saas.events.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.common.exception.BusinessException;
import com.saas.events.application.dto.response.NotificationResponse;
import com.saas.events.application.mapper.NotificationMapper;
import com.saas.events.application.service.GlobalLaunchService;
import com.saas.events.domain.model.NotificationTemplate;
import com.saas.events.domain.port.out.INotificationTemplateRepositoryPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Lanzamiento global. Solo lista y lanza notificaciones marcadas como globales.
 */
@RestController
@RequestMapping("/notification/global")
@RequiredArgsConstructor
public class GlobalLaunchController {

    private final GlobalLaunchService service;
    private final INotificationTemplateRepositoryPort templates;
    private final NotificationMapper mapper;

    /**
     * Las unicas lanzables. La pantalla no ofrece nada mas.
     *
     * Devuelve la MISMA forma que {@code GET /notification/notifications},
     * incluidos los canales: la pantalla decide con ellos si una notificacion se
     * puede enviar (sin plantillas no hay nada que mandar) y no deberia tener que
     * recomponer ese dato con una segunda llamada por fila.
     */
    @GetMapping("/launchable")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> launchable() {
        List<NotificationResponse> out = service.launchable().stream()
                .map(n -> {
                    NotificationResponse base = mapper.toResponse(n);
                    List<String> channels = templates.findByNotificationId(n.getId()).stream()
                            .map(NotificationTemplate::getTypeCode)
                            .distinct()
                            .toList();
                    return new NotificationResponse(base.id(), base.code(), base.name(),
                            base.description(), channels, base.isGlobal(),
                            base.enabled(), base.visible());
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(out));
    }

    /** Recuento por medio ANTES de lanzar. -1 significa "no se pudo consultar", no cero. */
    @GetMapping("/audience")
    public ResponseEntity<ApiResponse<Map<String, Long>>> audience(@RequestParam List<String> channels) {
        return ResponseEntity.ok(ApiResponse.success(service.audience(channels)));
    }

    public record LaunchRequest(
            @NotBlank String notificationCode,
            @NotEmpty List<String> channels,
            /** Debe coincidir con notificationCode: es la confirmacion escrita. */
            @NotBlank String confirmCode) {}

    /**
     * Lanza a toda la base.
     *
     * Exige escribir el codigo de la notificacion como confirmacion. No es
     * burocracia: es la unica accion del panel que le escribe a todos los
     * clientes de golpe y no tiene vuelta atras. Un boton de un clic aqui es un
     * accidente esperando ocurrir.
     */
    @PostMapping("/launch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GlobalLaunchService.LaunchResult>> launch(
            @Valid @RequestBody LaunchRequest req) {
        if (!req.notificationCode().equals(req.confirmCode())) {
            throw new BusinessException(
                    "Para lanzar, escribe exactamente el código de la notificación: " + req.notificationCode());
        }
        return ResponseEntity.ok(ApiResponse.success(
                service.launch(req.notificationCode(), req.channels())));
    }
}
