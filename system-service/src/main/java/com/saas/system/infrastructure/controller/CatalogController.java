package com.saas.system.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.common.model.BaseCatalogDomain;
import com.saas.common.service.BaseCatalogService;
import com.saas.system.application.dto.request.CatalogRequest;
import com.saas.system.application.dto.response.CatalogResponse;
import com.saas.system.infrastructure.catalog.CatalogRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller unico para todos los catalogos del sistema.
 *
 * <p>Enruta dinamicamente cada peticion al servicio correspondiente segun el
 * {@code catalogName} en la URL. Agregar un catalogo nuevo no requiere tocar
 * este controller: el {@link CatalogRegistry} lo detecta automaticamente al
 * iniciar la aplicacion.</p>
 *
 * <p>Endpoints:</p>
 * <pre>
 *   GET    /list/{name}                 todos los visibles
 *   GET    /list/{name}/enabled         solo enabled (y visibles)
 *   GET    /list/{name}/code/{code}     uno por codigo
 *   GET    /list/{name}/{id}            uno por id
 *   POST   /list/{name}                 crear (ADMIN)
 *   PUT    /list/{name}/{id}            actualizar (ADMIN)
 *   DELETE /list/{name}/{id}            soft-delete (ADMIN)
 *   GET    /list                        lista todos los catalogos disponibles
 * </pre>
 */
@RestController
@RequestMapping("/list")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogRegistry registry;

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> available() {
        return ResponseEntity.ok(ApiResponse.success(registry.listAvailable()));
    }

    @GetMapping("/{catalogName}")
    public ResponseEntity<ApiResponse<List<CatalogResponse>>> getAll(@PathVariable String catalogName) {
        List<? extends BaseCatalogDomain> items = items(catalogName);
        return ResponseEntity.ok(ApiResponse.success(items.stream().map(this::toResponse).toList()));
    }

    @GetMapping("/{catalogName}/enabled")
    public ResponseEntity<ApiResponse<List<CatalogResponse>>> getEnabled(@PathVariable String catalogName) {
        List<CatalogResponse> result = items(catalogName).stream()
                .filter(d -> Boolean.TRUE.equals(d.getEnabled()))
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{catalogName}/{id}")
    public ResponseEntity<ApiResponse<CatalogResponse>> getById(@PathVariable String catalogName,
                                                                @PathVariable UUID id) {
        BaseCatalogDomain item = (BaseCatalogDomain) registry.get(catalogName).getById(id);
        return ResponseEntity.ok(ApiResponse.success(toResponse(item)));
    }

    @GetMapping("/{catalogName}/code/{code}")
    public ResponseEntity<ApiResponse<CatalogResponse>> getByCode(@PathVariable String catalogName,
                                                                  @PathVariable String code) {
        BaseCatalogDomain item = (BaseCatalogDomain) registry.get(catalogName).getByCode(code);
        return ResponseEntity.ok(ApiResponse.success(toResponse(item)));
    }

    @PostMapping("/{catalogName}")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ResponseEntity<ApiResponse<CatalogResponse>> create(@PathVariable String catalogName,
                                                                @Valid @RequestBody CatalogRequest req) {
        BaseCatalogService service = registry.get(catalogName);
        BaseCatalogDomain entity = service.newInstance();
        apply(req, entity);
        BaseCatalogDomain saved = (BaseCatalogDomain) service.create(entity);
        return ResponseEntity.ok(ApiResponse.created(toResponse(saved)));
    }

    @PutMapping("/{catalogName}/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ResponseEntity<ApiResponse<CatalogResponse>> update(@PathVariable String catalogName,
                                                                @PathVariable UUID id,
                                                                @Valid @RequestBody CatalogRequest req) {
        BaseCatalogService service = registry.get(catalogName);
        BaseCatalogDomain existing = (BaseCatalogDomain) service.getById(id);
        apply(req, existing);
        BaseCatalogDomain updated = (BaseCatalogDomain) service.update(id, existing);
        return ResponseEntity.ok(ApiResponse.success(toResponse(updated)));
    }

    @DeleteMapping("/{catalogName}/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String catalogName,
                                                     @PathVariable UUID id) {
        registry.get(catalogName).delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Item deshabilitado"));
    }

    // ---------- helpers ----------

    @SuppressWarnings("unchecked")
    private List<? extends BaseCatalogDomain> items(String catalogName) {
        return (List<? extends BaseCatalogDomain>) registry.get(catalogName).getAll();
    }

    private void apply(CatalogRequest req, BaseCatalogDomain target) {
        target.setCode(req.code());
        target.setName(req.name());
        target.setValue(req.value());
        target.setDisplayOrder(req.displayOrder());
    }

    private CatalogResponse toResponse(BaseCatalogDomain d) {
        return new CatalogResponse(
                d.getId(),
                d.getCode(),
                d.getName(),
                d.getValue(),
                d.getDisplayOrder(),
                d.getEnabled(),
                d.getVisible(),
                d.getCreatedDate(),
                d.getAuditDate()
        );
    }
}
