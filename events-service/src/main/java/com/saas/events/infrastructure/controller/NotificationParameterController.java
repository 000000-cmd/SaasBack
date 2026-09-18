package com.saas.events.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.events.application.dto.request.NotificationParameterRequest;
import com.saas.events.application.dto.response.NotificationParameterResponse;
import com.saas.events.application.mapper.NotificationParameterMapper;
import com.saas.events.domain.model.NotificationParameter;
import com.saas.events.domain.port.in.INotificationParameterUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notification/parameters")
@RequiredArgsConstructor
public class NotificationParameterController {

    private final INotificationParameterUseCase useCase;
    private final NotificationParameterMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationParameterResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                useCase.getAll().stream().map(mapper::toResponse).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationParameterResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationParameterResponse>> create(
            @Valid @RequestBody NotificationParameterRequest req) {
        NotificationParameter created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationParameterResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody NotificationParameterRequest req) {
        NotificationParameter existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Parámetro deshabilitado"));
    }
}
