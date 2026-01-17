package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.system.application.dto.request.DynamicListRequest;
import com.saas.system.application.dto.request.ListDefinitionRequest;
import com.saas.system.application.dto.response.DynamicListResponse;
import com.saas.system.application.dto.response.ListDefinitionResponse;
import com.saas.system.application.mapper.DynamicListMapper;
import com.saas.system.application.mapper.ListDefinitionMapper;
import com.saas.system.domain.model.DynamicList;
import com.saas.system.domain.model.ListDefinition;
import com.saas.system.domain.port.in.IDynamicListUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de listas dinámicas del sistema.
 *
 * Endpoints principales:
 * - GET    /api/system/lists                     -> Obtener todas las definiciones de listas
 * - POST   /api/system/lists                     -> Crear nueva definición de lista
 * - GET    /api/system/lists/{listType}          -> Obtener todos los items de una lista
 * - GET    /api/system/lists/{listType}/enabled  -> Obtener solo items habilitados
 * - POST   /api/system/lists/{listType}          -> Crear item en una lista
 * - GET    /api/system/lists/{listType}/{id}     -> Obtener item por ID
 * - GET    /api/system/lists/{listType}/code/{code} -> Obtener item por código
 * - PUT    /api/system/lists/{listType}/{id}     -> Actualizar item
 * - PATCH  /api/system/lists/{listType}/{id}/status -> Cambiar estado
 * - DELETE /api/system/lists/{listType}/{id}     -> Eliminar item
 */
@RestController
@RequestMapping("/api/system/lists")
@RequiredArgsConstructor
public class DynamicListController {

    private final IDynamicListUseCase dynamicListUseCase;
    private final DynamicListMapper dynamicListMapper;
    private final ListDefinitionMapper listDefinitionMapper;

    // ==================== Endpoints para definiciones de listas ====================

    /**
     * Obtiene todas las definiciones de listas disponibles.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ListDefinitionResponse>>> getAllListDefinitions() {
        List<ListDefinition> definitions = dynamicListUseCase.getAllListDefinitions();
        List<ListDefinitionResponse> response = definitions.stream()
                .map(listDefinitionMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Crea una nueva definición de lista.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ListDefinitionResponse>> createListDefinition(
            @Valid @RequestBody ListDefinitionRequest request) {
        ListDefinition domain = listDefinitionMapper.toDomain(request);
        ListDefinition created = dynamicListUseCase.createListDefinition(domain);
        ListDefinitionResponse response = listDefinitionMapper.toResponse(created);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Definición de lista creada exitosamente"));
    }

    /**
     * Actualiza una definición de lista.
     */
    @PutMapping("/definition/{id}")
    public ResponseEntity<ApiResponse<ListDefinitionResponse>> updateListDefinition(
            @PathVariable String id,
            @Valid @RequestBody ListDefinitionRequest request) {
        ListDefinition domain = listDefinitionMapper.toDomain(request);
        ListDefinition updated = dynamicListUseCase.updateListDefinition(id, domain);
        ListDefinitionResponse response = listDefinitionMapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response, "Definición de lista actualizada exitosamente"));
    }

    /**
     * Elimina una definición de lista.
     */
    @DeleteMapping("/definition/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteListDefinition(@PathVariable String id) {
        dynamicListUseCase.deleteListDefinition(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Definición de lista eliminada exitosamente"));
    }

    // ==================== Endpoints para items de listas ====================

    /**
     * Obtiene todos los items de una lista específica.
     *
     * @param listType Identificador de la lista (ej: document-types, gender-types)
     */
    @GetMapping("/{listType}")
    public ResponseEntity<ApiResponse<List<DynamicListResponse>>> getAllItems(
            @PathVariable String listType) {
        List<DynamicList> items = dynamicListUseCase.getAllItems(listType);
        List<DynamicListResponse> response = items.stream()
                .map(dynamicListMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Obtiene solo los items habilitados de una lista.
     */
    @GetMapping("/{listType}/enabled")
    public ResponseEntity<ApiResponse<List<DynamicListResponse>>> getEnabledItems(
            @PathVariable String listType) {
        List<DynamicList> items = dynamicListUseCase.getEnabledItems(listType);
        List<DynamicListResponse> response = items.stream()
                .map(dynamicListMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Obtiene un item por su ID.
     */
    @GetMapping("/{listType}/id/{id}")
    public ResponseEntity<ApiResponse<DynamicListResponse>> getItemById(
            @PathVariable String listType,
            @PathVariable String id) {
        DynamicList item = dynamicListUseCase.getItemById(listType, id);
        DynamicListResponse response = dynamicListMapper.toResponse(item);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Obtiene un item por su código.
     */
    @GetMapping("/{listType}/code/{code}")
    public ResponseEntity<ApiResponse<DynamicListResponse>> getItemByCode(
            @PathVariable String listType,
            @PathVariable String code) {
        DynamicList item = dynamicListUseCase.getItemByCode(listType, code);
        DynamicListResponse response = dynamicListMapper.toResponse(item);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Crea un nuevo item en una lista.
     */
    @PostMapping("/{listType}")
    public ResponseEntity<ApiResponse<DynamicListResponse>> createItem(
            @PathVariable String listType,
            @Valid @RequestBody DynamicListRequest request) {
        DynamicList domain = dynamicListMapper.toDomain(request);
        DynamicList created = dynamicListUseCase.createItem(listType, domain);
        DynamicListResponse response = dynamicListMapper.toResponse(created);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Item creado exitosamente"));
    }

    /**
     * Actualiza un item existente.
     */
    @PutMapping("/{listType}/{id}")
    public ResponseEntity<ApiResponse<DynamicListResponse>> updateItem(
            @PathVariable String listType,
            @PathVariable String id,
            @Valid @RequestBody DynamicListRequest request) {
        DynamicList domain = dynamicListMapper.toDomain(request);
        DynamicList updated = dynamicListUseCase.updateItem(listType, id, domain);
        DynamicListResponse response = dynamicListMapper.toResponse(updated);
        return ResponseEntity.ok(ApiResponse.success(response, "Item actualizado exitosamente"));
    }

    /**
     * Cambia el estado habilitado de un item.
     */
    @PatchMapping("/{listType}/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleItemStatus(
            @PathVariable String listType,
            @PathVariable String id,
            @RequestParam boolean enabled) {
        dynamicListUseCase.toggleItemEnabled(listType, id, enabled);
        return ResponseEntity.ok(ApiResponse.success(null,
                enabled ? "Item habilitado" : "Item deshabilitado"));
    }

    /**
     * Elimina un item de una lista (soft delete).
     */
    @DeleteMapping("/{listType}/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable String listType,
            @PathVariable String id) {
        dynamicListUseCase.deleteItem(listType, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Item eliminado exitosamente"));
    }
}