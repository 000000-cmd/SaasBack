package com.saas.events.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.events.application.dto.request.NotificationTemplateRequest;
import com.saas.events.application.dto.request.PreviewRequest;
import com.saas.events.application.dto.response.NotificationTemplateResponse;
import com.saas.events.application.dto.response.PreviewResponse;
import com.saas.events.application.mapper.NotificationTemplateMapper;
import com.saas.events.application.service.TemplateRenderer;
import com.saas.events.domain.model.ChannelType;
import com.saas.events.domain.model.NotificationTemplate;
import com.saas.events.domain.port.in.INotificationTemplateUseCase;
import com.saas.events.domain.port.out.INotificationTemplateRepositoryPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notification/templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final TemplateRenderer renderer;
    private final INotificationTemplateUseCase useCase;
    private final NotificationTemplateMapper mapper;
    private final INotificationTemplateRepositoryPort repositoryPort;

    /**
     * Renderiza sin guardar ni enviar. Lo consume la previsualizacion del panel,
     * que asi no reimplementa la sustitucion: lo que se ve aqui es, byte a byte,
     * lo que saldria en el envio real.
     */
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<PreviewResponse>> preview(@Valid @RequestBody PreviewRequest req) {
        ChannelType channel = ChannelType.from(req.typeCode());
        List<String> missing = new ArrayList<>(renderer.missingParameters(req.body(), req.data()));
        for (String code : renderer.missingParameters(req.subject(), req.data())) {
            if (!missing.contains(code)) missing.add(code);
        }
        return ResponseEntity.ok(ApiResponse.success(new PreviewResponse(
                renderer.render(req.subject(), req.data(), channel),
                renderer.render(req.body(), req.data(), channel),
                missing)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationTemplateResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                useCase.getAll().stream().map(mapper::toResponse).toList()));
    }

    /** Plantillas todavia sin notificacion: alimenta el selector de asociacion. */
    @GetMapping("/unassigned")
    public ResponseEntity<ApiResponse<List<NotificationTemplateResponse>>> unassigned() {
        return ResponseEntity.ok(ApiResponse.success(
                repositoryPort.findUnassigned().stream().map(mapper::toResponse).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> create(
            @Valid @RequestBody NotificationTemplateRequest req) {
        NotificationTemplate created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody NotificationTemplateRequest req) {
        NotificationTemplate existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Plantilla deshabilitada"));
    }

    /**
     * Asocia la plantilla a una notificacion. Endpoint dedicado y no un PUT
     * completo porque desasociar es poner el campo a null, y un PUT parcial no
     * distingue "no lo mandes" de "ponlo a null".
     *
     * Usa {@code repositoryPort.save(...)} en vez de {@code useCase.update(...)}
     * a proposito: GenericCrudService.update() vuelve a leer la entidad de BD y
     * solo conserva lo que applyChanges copia explicitamente, y ademas
     * BaseJpaRepositoryAdapter.update() hace el merge final con
     * NullValuePropertyMappingStrategy.IGNORE (via IBaseMapper). Esas dos capas
     * de "ignorar null" hacen que un null JAMAS sobreviva por ese camino, sin
     * importar como se escriba applyChanges. save() hace un toEntity() completo
     * (sin merge), asi que es el unico camino de este framework que persiste un
     * null. Efecto colateral aceptado: esta escritura no pasa por
     * GenericCrudService, asi que no emite auditoria para este cambio puntual.
     */
    @PutMapping("/{id}/notification/{notificationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> assign(
            @PathVariable UUID id, @PathVariable UUID notificationId) {
        NotificationTemplate tpl = useCase.getById(id);
        tpl.setNotificationId(notificationId);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(repositoryPort.save(tpl))));
    }

    @DeleteMapping("/{id}/notification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> unassign(@PathVariable UUID id) {
        NotificationTemplate tpl = useCase.getById(id);
        tpl.setNotificationId(null);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(repositoryPort.save(tpl))));
    }
}
