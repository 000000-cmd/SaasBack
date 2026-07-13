package com.saas.business.infrastructure.controller;

import com.saas.business.application.dto.response.OwnerBusinessResponse;
import com.saas.business.domain.model.BusinessOwner;
import com.saas.business.domain.port.in.IBusinessOwnerUseCase;
import com.saas.business.infrastructure.client.ThirdPartyClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints S2S (no traversed por gateway). Resuelve el businessId del dueño a
 * partir de su userId, para que el auth lo selle como claim en el JWT sin que
 * cada request downstream tenga que hacer el lookup (userId → persona →
 * business_owner → business).
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalBusinessController {

    private final ThirdPartyClient thirdPartyClient;
    private final IBusinessOwnerUseCase ownerUseCase;

    @GetMapping("/owner-business")
    public OwnerBusinessResponse ownerBusiness(@RequestParam UUID userId) {
        UUID businessId = null;
        try {
            UUID thirdPartyId = thirdPartyClient.personByUser(userId).id();
            businessId = ownerUseCase.findByThirdParty(thirdPartyId).stream()
                    .findFirst()
                    .map(BusinessOwner::getBusinessId)
                    .orElse(null);
        } catch (Exception ex) {
            // Sin persona vinculada o sin owner: aún no aprovisionó. No es error.
            log.debug("Sin negocio para userId={}: {}", userId, ex.getMessage());
        }
        return new OwnerBusinessResponse(businessId);
    }
}
