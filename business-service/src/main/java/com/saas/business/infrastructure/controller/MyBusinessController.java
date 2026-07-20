package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.response.BusinessResponse;
import com.saas.business.application.mapper.BusinessMapper;
import com.saas.business.domain.port.in.IBusinessOwnerUseCase;
import com.saas.business.domain.port.in.IBusinessUseCase;
import com.saas.business.application.service.PersonLookupService;
import com.saas.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * "Mi empresa": empresas del usuario logueado (dueño). Resuelve la cadena
 * userId → persona (read model de ES, respaldo thirdparty) → business_owner → business.
 * Ruta efectiva: {@code GET /business/mine?userId=...} (requiere JWT).
 */
@RestController
@RequestMapping("/mine")
@RequiredArgsConstructor
@Slf4j
public class MyBusinessController {

    private final PersonLookupService personLookup;
    private final IBusinessOwnerUseCase ownerUseCase;
    private final IBusinessUseCase businessUseCase;
    private final BusinessMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusinessResponse>>> myBusinesses(@RequestParam UUID userId) {
        // Sin persona vinculada (aún no aprovisionó): lista vacía, no es error.
        UUID thirdPartyId = personLookup.thirdPartyIdByUser(userId).orElse(null);
        if (thirdPartyId == null) return ResponseEntity.ok(ApiResponse.success(List.of()));

        List<BusinessResponse> businesses = ownerUseCase.findByThirdParty(thirdPartyId).stream()
                .map(o -> businessUseCase.getById(o.getBusinessId()))
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(businesses));
    }
}
