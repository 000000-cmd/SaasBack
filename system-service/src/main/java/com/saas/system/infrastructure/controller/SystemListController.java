package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.request.SystemListRequest;
import com.saas.system.application.dto.response.SystemListResponse;
import com.saas.system.application.mapper.SystemListMapper;
import com.saas.system.domain.model.SystemList;
import com.saas.system.domain.port.in.ISystemListUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Meta-registro de catalogos del sistema.
 *
 * <p>Cada registro en {@code system_list} declara un catalogo existente
 * (TIPOS_DOCUMENTO, ESTADOS_REGISTRO, GENEROS, ...). Los items de cada
 * catalogo viven en su tabla propia y se acceden via {@link CatalogController}
 * en {@code /list/{catalogName}}.</p>
 */
@RestController
@RequestMapping("/system-lists")
@RequiredArgsConstructor
public class SystemListController {

    private final ISystemListUseCase listUseCase;
    private final SystemListMapper listMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemListResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                listUseCase.getAll().stream().map(listMapper::toResponse).toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemListResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(listMapper.toResponse(listUseCase.getById(id))));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<SystemListResponse>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(listMapper.toResponse(listUseCase.getByCode(code))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemListResponse>> create(@Valid @RequestBody SystemListRequest req) {
        SystemList created = listUseCase.create(listMapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(listMapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemListResponse>> update(@PathVariable UUID id, @Valid @RequestBody SystemListRequest req) {
        SystemList existing = listUseCase.getById(id);
        listMapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(listMapper.toResponse(listUseCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        listUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Lista deshabilitada"));
    }
}
