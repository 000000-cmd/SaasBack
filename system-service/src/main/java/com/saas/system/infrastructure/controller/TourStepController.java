package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.common.security.IUserPrincipal;
import com.saas.system.application.dto.request.TourStepRequest;
import com.saas.system.application.dto.response.TourStepResponse;
import com.saas.system.application.mapper.TourStepMapper;
import com.saas.system.domain.model.Role;
import com.saas.system.domain.model.TourStep;
import com.saas.system.domain.port.in.IRoleUseCase;
import com.saas.system.domain.port.in.ITourStepUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Configuracion del tour guiado. El CRUD es de ADMIN; {@code /me} lo usa el front. */
@RestController
@RequestMapping("/tour-steps")
@RequiredArgsConstructor
public class TourStepController {

    private final ITourStepUseCase useCase;
    private final IRoleUseCase roleUseCase;
    private final TourStepMapper mapper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TourStepResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                useCase.getAllOrdered().stream().map(mapper::toResponse).toList()));
    }

    /** Pasos visibles para el usuario actual, ya ordenados. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<TourStepResponse>>> mySteps(
            @AuthenticationPrincipal IUserPrincipal principal) {
        Set<UUID> roleIds = roleUseCase.getAll().stream()
                .filter(r -> principal.getRoles().contains(r.getCode()))
                .map(Role::getId)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(ApiResponse.success(
                useCase.getStepsForRoles(roleIds).stream().map(mapper::toResponse).toList()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TourStepResponse>> create(@Valid @RequestBody TourStepRequest req) {
        TourStep created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TourStepResponse>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody TourStepRequest req) {
        TourStep existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        // Explicitos: el mapper ignora nulls y estos deben poder vaciarse.
        existing.setMenuId(req.menuId());
        existing.setAnchor(req.anchor());
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Paso deshabilitado"));
    }
}
