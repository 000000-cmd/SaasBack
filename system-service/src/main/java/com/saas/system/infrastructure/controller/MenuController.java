package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.request.MenuRequest;
import com.saas.system.application.dto.response.MenuResponse;
import com.saas.system.application.mapper.MenuMapper;
import com.saas.system.domain.model.Menu;
import com.saas.system.domain.port.in.IMenuUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de Menús.
 */
@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class MenuController {

    private final IMenuUseCase menuUseCase;
    private final MenuMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<MenuResponse>> create(@Valid @RequestBody MenuRequest request) {
        Menu domain = mapper.toDomain(request);
        Menu created = menuUseCase.create(domain);
        MenuResponse response = mapper.toResponse(created);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getAll() {
        List<Menu> menus = menuUseCase.getAll();
        List<MenuResponse> response = mapper.toResponseList(menus);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/root")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getRootMenus() {
        List<Menu> menus = menuUseCase.getRootMenus();
        List<MenuResponse> response = mapper.toResponseList(menus);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/children/{parentId}")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getChildren(@PathVariable String parentId) {
        List<Menu> menus = menuUseCase.getByParentId(parentId);
        List<MenuResponse> response = mapper.toResponseList(menus);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<MenuResponse>> getByCode(@PathVariable String code) {
        Menu menu = menuUseCase.getByCode(code);
        MenuResponse response = mapper.toResponse(menu);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<MenuResponse>> getById(@PathVariable String id) {
        Menu menu = menuUseCase.getById(id);
        MenuResponse response = mapper.toResponse(menu);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody MenuRequest request) {
        Menu domain = mapper.toDomain(request);
        Menu updated = menuUseCase.update(id, domain);
        MenuResponse response = mapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response, "Menú actualizado exitosamente"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(
            @PathVariable String id,
            @RequestParam boolean enabled) {
        menuUseCase.toggleEnabled(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(null,
                enabled ? "Menú habilitado" : "Menú deshabilitado"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        menuUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Menú eliminado exitosamente"));
    }
}