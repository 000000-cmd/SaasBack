package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.application.dto.request.SystemListItemRequest;
import com.saas.system.application.dto.request.SystemListRequest;
import com.saas.system.application.dto.response.SystemListItemResponse;
import com.saas.system.application.dto.response.SystemListResponse;
import com.saas.system.application.mapper.SystemListItemMapper;
import com.saas.system.application.mapper.SystemListMapper;
import com.saas.system.domain.model.SystemList;
import com.saas.system.domain.model.SystemListItem;
import com.saas.system.domain.port.in.ISystemListItemUseCase;
import com.saas.system.domain.port.in.ISystemListUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/system-lists")
@RequiredArgsConstructor
public class SystemListController {

    private final ISystemListUseCase listUseCase;
    private final ISystemListItemUseCase itemUseCase;
    private final SystemListMapper listMapper;
    private final SystemListItemMapper itemMapper;

    // --- Listas ---

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemListResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(listUseCase.getAll().stream().map(listMapper::toResponse).toList()));
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

    // --- Items dentro de una lista ---

    @GetMapping("/{listId}/items")
    public ResponseEntity<ApiResponse<List<SystemListItemResponse>>> getItems(@PathVariable UUID listId) {
        return ResponseEntity.ok(ApiResponse.success(
                itemUseCase.getByListId(listId).stream().map(itemMapper::toResponse).toList()));
    }

    /** Lookup tipico desde el frontend: /system-lists/code/TIPOS_DOCUMENTO/items */
    @GetMapping("/code/{listCode}/items")
    public ResponseEntity<ApiResponse<List<SystemListItemResponse>>> getItemsByListCode(@PathVariable String listCode) {
        SystemList list = listUseCase.getByCode(listCode);
        return ResponseEntity.ok(ApiResponse.success(
                itemUseCase.getByListId(list.getId()).stream().map(itemMapper::toResponse).toList()));
    }

    @GetMapping("/code/{listCode}/items/{itemCode}")
    public ResponseEntity<ApiResponse<SystemListItemResponse>> getItemByCodes(@PathVariable String listCode,
                                                                                @PathVariable String itemCode) {
        SystemListItem item = itemUseCase.getByListCodeAndItemCode(listCode, itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Item de lista", "Code", listCode + "/" + itemCode));
        return ResponseEntity.ok(ApiResponse.success(itemMapper.toResponse(item)));
    }

    @PostMapping("/{listId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemListItemResponse>> addItem(@PathVariable UUID listId,
                                                                         @Valid @RequestBody SystemListItemRequest req) {
        SystemListItem created = itemUseCase.createInList(listId, itemMapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(itemMapper.toResponse(created)));
    }

    @PutMapping("/{listId}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemListItemResponse>> updateItem(@PathVariable UUID listId,
                                                                           @PathVariable UUID itemId,
                                                                           @Valid @RequestBody SystemListItemRequest req) {
        SystemListItem existing = itemUseCase.getById(itemId);
        itemMapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(itemMapper.toResponse(itemUseCase.update(itemId, existing))));
    }

    @DeleteMapping("/{listId}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable UUID listId, @PathVariable UUID itemId) {
        itemUseCase.delete(itemId);
        return ResponseEntity.ok(ApiResponse.success(null, "Item deshabilitado"));
    }
}
