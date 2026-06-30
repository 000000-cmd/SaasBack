package com.saas.thirdparty.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.thirdparty.application.dto.request.ThirdPartyRequest;
import com.saas.thirdparty.application.dto.response.ThirdPartyDetailResponse;
import com.saas.thirdparty.application.dto.response.ThirdPartyResponse;
import com.saas.thirdparty.application.mapper.ThirdPartyAddressMapper;
import com.saas.thirdparty.application.mapper.ThirdPartyContactMapper;
import com.saas.thirdparty.application.mapper.ThirdPartyMapper;
import com.saas.thirdparty.application.service.ThirdPartyReindexPublisher;
import com.saas.thirdparty.domain.model.ThirdParty;
import com.saas.thirdparty.domain.port.in.IThirdPartyAddressUseCase;
import com.saas.thirdparty.domain.port.in.IThirdPartyContactUseCase;
import com.saas.thirdparty.domain.port.in.IThirdPartyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API REST de Terceros. Context-path {@code /thirdparty} → ruta efectiva
 * {@code /thirdparty/third-parties}. El {@code businessId} (cuando aplica) viaja
 * por el header {@code X-Business-Id} y lo sella el estándar de auditoría.
 */
@RestController
@RequestMapping("/third-parties")
@RequiredArgsConstructor
public class ThirdPartyController {

    private final IThirdPartyUseCase useCase;
    private final ThirdPartyMapper mapper;
    private final IThirdPartyContactUseCase contactUseCase;
    private final ThirdPartyContactMapper contactMapper;
    private final IThirdPartyAddressUseCase addressUseCase;
    private final ThirdPartyAddressMapper addressMapper;
    private final ThirdPartyReindexPublisher reindexPublisher;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ThirdPartyResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.getAll())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ThirdPartyResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }

    /** Info COMPLETA y anidada del tercero (datos base + contactos + direcciones). */
    @GetMapping("/{id}/full")
    public ResponseEntity<ApiResponse<ThirdPartyDetailResponse>> getFull(@PathVariable UUID id) {
        ThirdPartyResponse tp = mapper.toResponse(useCase.getById(id));
        ThirdPartyDetailResponse detail = new ThirdPartyDetailResponse(
                tp,
                contactMapper.toResponseList(contactUseCase.findByThirdParty(id)),
                addressMapper.toResponseList(addressUseCase.findByThirdParty(id)));
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    /** Reindexa el tercero (read-model de busqueda) en Elasticsearch. */
    @PostMapping("/{id}/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reindex(@PathVariable UUID id) {
        useCase.getById(id); // 404 si no existe
        reindexPublisher.reindex(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Tercero reindexado"));
    }

    @GetMapping("/document")
    public ResponseEntity<ApiResponse<ThirdPartyResponse>> getByDocument(
            @RequestParam UUID documentTypeId,
            @RequestParam String documentNumber) {
        return useCase.findByDocument(documentTypeId, documentNumber)
                .map(t -> ResponseEntity.ok(ApiResponse.success(mapper.toResponse(t))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("Tercero no encontrado", 404)));
    }

    @GetMapping("/document/exists")
    public ResponseEntity<ApiResponse<Boolean>> existsByDocument(
            @RequestParam UUID documentTypeId,
            @RequestParam String documentNumber) {
        return ResponseEntity.ok(ApiResponse.success(
                useCase.existsByDocument(documentTypeId, documentNumber)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ThirdPartyResponse>> create(@Valid @RequestBody ThirdPartyRequest req) {
        ThirdParty created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ThirdPartyResponse>> update(@PathVariable UUID id,
                                                                  @Valid @RequestBody ThirdPartyRequest req) {
        ThirdParty existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Tercero deshabilitado"));
    }
}
