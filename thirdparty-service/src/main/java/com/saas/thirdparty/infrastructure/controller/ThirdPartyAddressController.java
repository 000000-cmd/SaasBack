package com.saas.thirdparty.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.thirdparty.application.dto.request.ThirdPartyAddressRequest;
import com.saas.thirdparty.application.dto.response.ThirdPartyAddressResponse;
import com.saas.thirdparty.application.mapper.ThirdPartyAddressMapper;
import com.saas.thirdparty.domain.model.ThirdPartyAddress;
import com.saas.thirdparty.domain.port.in.IThirdPartyAddressUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/third-party-addresses")
@RequiredArgsConstructor
public class ThirdPartyAddressController {

    private final IThirdPartyAddressUseCase useCase;
    private final ThirdPartyAddressMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ThirdPartyAddressResponse>>> byThirdParty(
            @RequestParam UUID thirdPartyId) {
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toResponseList(useCase.findByThirdParty(thirdPartyId))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ThirdPartyAddressResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.getById(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ThirdPartyAddressResponse>> create(
            @Valid @RequestBody ThirdPartyAddressRequest req) {
        ThirdPartyAddress created = useCase.create(mapper.toDomain(req));
        return ResponseEntity.ok(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ThirdPartyAddressResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ThirdPartyAddressRequest req) {
        ThirdPartyAddress existing = useCase.getById(id);
        mapper.updateDomain(req, existing);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(useCase.update(id, existing))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        useCase.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Direccion eliminada"));
    }
}
