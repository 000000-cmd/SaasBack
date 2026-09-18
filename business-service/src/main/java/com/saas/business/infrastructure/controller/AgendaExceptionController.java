package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.AgendaExceptionRequest;
import com.saas.business.application.dto.response.AgendaExceptionResponse;
import com.saas.business.domain.model.AgendaException;
import com.saas.business.domain.model.AgendaExceptionKind;
import com.saas.business.domain.port.in.IAgendaExceptionUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Festivos, vacaciones, incapacidades y bloqueos sueltos.
 *
 * <p>Sin mapper MapStruct a proposito: son siete campos y la respuesta lleva
 * una etiqueta calculada. Un mapper para esto seria mas codigo que el mapeo.</p>
 */
@RestController
@RequestMapping("/agenda-exceptions")
@RequiredArgsConstructor
public class AgendaExceptionController {

    private final IAgendaExceptionUseCase useCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgendaExceptionResponse>>> inRange(
            @RequestParam UUID businessId,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return ResponseEntity.ok(ApiResponse.success(
                useCase.inRange(businessId, from, to).stream()
                        .map(AgendaExceptionController::toResponse).toList()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<AgendaExceptionResponse>> create(
            @Valid @RequestBody AgendaExceptionRequest req) {
        return ResponseEntity.ok(ApiResponse.created(toResponse(useCase.create(toDomain(req)))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<AgendaExceptionResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody AgendaExceptionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(toResponse(useCase.update(id, toDomain(req)))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Bloqueo eliminado"));
    }

    private static AgendaException toDomain(AgendaExceptionRequest r) {
        AgendaException e = AgendaException.builder()
                .branchId(r.branchId())
                .employeeId(r.employeeId())
                .startUtc(r.startUtc())
                .endUtc(r.endUtc())
                .kind(r.kind() == null ? AgendaExceptionKind.BLOCK : r.kind())
                .reason(r.reason())
                .build();
        e.setBusinessId(r.businessId());
        if (r.enabled() != null) e.setEnabled(r.enabled());
        return e;
    }

    private static AgendaExceptionResponse toResponse(AgendaException e) {
        return new AgendaExceptionResponse(
                e.getId(), e.getBusinessId(), e.getBranchId(), e.getEmployeeId(),
                e.getStartUtc(), e.getEndUtc(),
                e.getKind().name(), e.getKind().legible(),
                e.getReason(), e.getEnabled());
    }
}
