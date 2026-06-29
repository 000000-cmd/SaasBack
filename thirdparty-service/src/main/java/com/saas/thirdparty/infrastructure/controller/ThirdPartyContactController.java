package com.saas.thirdparty.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.thirdparty.application.dto.request.ThirdPartyContactRequest;
import com.saas.thirdparty.application.dto.response.ThirdPartyContactResponse;
import com.saas.thirdparty.application.mapper.ThirdPartyContactMapper;
import com.saas.thirdparty.domain.model.ThirdPartyContact;
import com.saas.thirdparty.domain.port.in.IThirdPartyContactUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/third-party-contacts")
@RequiredArgsConstructor
public class ThirdPartyContactController {

    private final IThirdPartyContactUseCase useCase;
    private final ThirdPartyContactMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ThirdPartyContactResponse>>> byThirdParty(
            @RequestParam UUID thirdPartyId) {
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponseList(useCase.findByThirdParty(thirdPartyId))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ThirdPartyContactResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ThirdPartyContactResponse>> create(
            @Valid @RequestBody ThirdPartyContactRequest req) {
        ThirdPartyContact created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ThirdPartyContactResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ThirdPartyContactRequest req) {
        ThirdPartyContact existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Contacto eliminado"));
    }
}
