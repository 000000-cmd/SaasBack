package com.saas.events.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.events.application.dto.request.NotificationRequest;
import com.saas.events.application.dto.response.NotificationResponse;
import com.saas.events.application.mapper.NotificationMapper;
import com.saas.events.domain.model.Notification;
import com.saas.events.domain.model.NotificationTemplate;
import com.saas.events.domain.port.in.INotificationUseCase;
import com.saas.events.domain.port.out.INotificationTemplateRepositoryPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notification/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationUseCase useCase;
    private final NotificationMapper mapper;
    private final INotificationTemplateRepositoryPort templates;

    /**
     * channels no sale del modelo de dominio de Notification: se rellena aqui
     * consultando las plantillas asociadas, para que la tabla del panel no
     * necesite una segunda llamada por fila.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list() {
        List<NotificationResponse> out = useCase.getAll().stream()
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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @Valid @RequestBody NotificationRequest req) {
        Notification created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody NotificationRequest req) {
        Notification existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notificación deshabilitada"));
    }
}
