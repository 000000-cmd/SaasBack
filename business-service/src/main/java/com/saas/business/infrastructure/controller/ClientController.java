package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.request.ClientRequest;
import com.saas.business.application.dto.response.ClientResponse;
import com.saas.business.application.mapper.ClientMapper;
import com.saas.business.domain.model.Client;
import com.saas.business.domain.port.in.IClientUseCase;
import com.saas.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {
    private final IClientUseCase useCase;
    private final ClientMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponseList(useCase.getAll())));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }
    @GetMapping("/third-party/{thirdPartyId}")
    public ResponseEntity<ApiResponse<ClientResponse>> byThirdParty(@PathVariable UUID thirdPartyId) {
        return useCase.findByThirdParty(thirdPartyId)
                .map(c -> ResponseEntity.ok(ApiResponse.success(mapper.toResponse(c))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("Cliente no encontrado", 404)));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<ClientResponse>> create(@Valid @RequestBody ClientRequest req) {
        Client created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> update(@PathVariable UUID id, @Valid @RequestBody ClientRequest req) {
        Client existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Cliente deshabilitado"));
    }
}
