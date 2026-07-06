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

    // Sin GET de listado aqui: el listado/busqueda es responsabilidad del read
    // model en search-service (/search/third-parties) — CQRS, no se duplica.

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

    // Sin /index-preview: el documento en ES ya es COMPLETO (base + contactos +
    // direcciones), asi que el comparador confronta directamente ES (searchDoc)
    // vs BD (/full). No se necesita una "proyeccion" aparte.

    /** Reindexa el tercero (read-model de busqueda) en Elasticsearch. */
    @PostMapping("/{id}/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reindex(@PathVariable UUID id) {
        useCase.getById(id); // 404 si no existe
        reindexPublisher.reindex(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Tercero reindexado"));
    }

    /** Tercero vinculado a una cuenta de usuario (el APK resuelve "mi persona"). */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<ApiResponse<ThirdPartyResponse>> getByUser(@PathVariable UUID userId) {
        return useCase.findByUserId(userId)
                .map(t -> ResponseEntity.ok(ApiResponse.success(mapper.toResponse(t))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("Tercero no encontrado", 404)));
    }

    /**
     * Habilita/deshabilita el ingreso con huella en el APK. La huella se valida
     * en el dispositivo; aqui solo se registra el consentimiento del tercero.
     */
    @PatchMapping("/{id}/biometric")
    public ResponseEntity<ApiResponse<ThirdPartyResponse>> setBiometric(@PathVariable UUID id,
                                                                        @RequestParam boolean enabled) {
        ThirdParty patch = ThirdParty.builder().biometricEnabled(enabled).build();
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponse(useCase.update(id, patch)),
                enabled ? "Ingreso con huella habilitado" : "Ingreso con huella deshabilitado"));
    }

    @GetMapping("/document")
    public ResponseEntity<ApiResponse<ThirdPartyResponse>> getByDocument(
            @RequestParam UUID documentTypeId,
            @RequestParam String documentNumber) {
        return useCase.findByDocument(documentTypeId, documentNumber)
                .map(t -> ResponseEntity.ok(ApiResponse.success(mapper.toResponse(t))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("Tercero no encontrado", 404)));
    }

    // El "exists" publico es redundante: /document ya responde 404 cuando no
    // existe. El pre-check S2S de duplicados vive en /internal (Feign).

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
