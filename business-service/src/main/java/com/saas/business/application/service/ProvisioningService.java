package com.saas.business.application.service;

import com.saas.business.application.dto.request.ProvisionRequest;
import com.saas.business.application.dto.response.ProvisionResponse;
import com.saas.business.domain.model.Business;
import com.saas.business.domain.model.BusinessDomain;
import com.saas.business.domain.model.BusinessOwner;
import com.saas.business.domain.port.in.IBusinessDomainUseCase;
import com.saas.business.domain.port.in.IBusinessOwnerUseCase;
import com.saas.business.domain.port.in.IBusinessUseCase;
import com.saas.business.infrastructure.client.ThirdPartyClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Orquesta el alta de un negocio: empresa + slug (local), persona del dueño
 * (thirdparty via Feign) y vínculo business_owner al 100%.
 *
 * <p>Nota de consistencia: la persona se crea en otro microservicio (llamada
 * remota), así que NO entra en la transacción local. Si un paso posterior falla,
 * la empresa/slug se revierten pero la persona remota quedaría huérfana. Para el
 * flujo feliz es suficiente; una compensación (saga) queda como mejora futura.</p>
 */
@Service
@RequiredArgsConstructor
public class ProvisioningService {

    private final IBusinessUseCase businessUseCase;
    private final IBusinessDomainUseCase domainUseCase;
    private final IBusinessOwnerUseCase ownerUseCase;
    private final ThirdPartyClient thirdPartyClient;

    @Transactional
    public ProvisionResponse provision(ProvisionRequest r) {
        Business business = businessUseCase.create(Business.builder()
                .businessTypeId(r.businessTypeId())
                .name(r.name())
                .legalName(r.legalName())
                .tradeName(r.tradeName())
                .documentTypeId(r.documentTypeId())
                .documentNumber(r.documentNumber())
                .logoUrl(r.logoUrl())
                .statusId(r.statusId())
                .primaryColor(r.primaryColor())
                .secondaryColor(r.secondaryColor())
                .build());

        BusinessDomain domain = domainUseCase.create(BusinessDomain.builder()
                .businessId(business.getId())
                .slug(r.slug())
                .isPrimary(true)
                .build());

        // Regla: UN solo tercero por usuario. Si el usuario ya tiene persona,
        // se ACTUALIZA (PUT); si no, se crea. Así una re-provisión no genera
        // terceros duplicados (que rompían el endpoint by-user).
        ThirdPartyClient.CreatePersonRequest personReq = new ThirdPartyClient.CreatePersonRequest(
                r.ownerDocumentTypeId(), r.ownerDocumentNumber(), r.ownerUserId(), business.getId(),
                r.ownerFirstName(), r.ownerSecondName(), r.ownerFirstLastName(), r.ownerSecondLastName(),
                r.ownerGenderId(), r.ownerBirthDate(), null);

        ThirdPartyClient.PersonResponse existing = findPersonByUser(r.ownerUserId());
        UUID personId = (existing != null)
                ? thirdPartyClient.updatePerson(existing.id(), personReq).id()
                : thirdPartyClient.createPerson(personReq).id();

        BusinessOwner owner = ownerUseCase.create(BusinessOwner.builder()
                .businessId(business.getId())
                .thirdPartyId(personId)
                .ownershipPercentage(new BigDecimal("100"))
                .startDate(LocalDate.now())
                .build());

        return new ProvisionResponse(business.getId(), domain.getSlug(), personId, owner.getId());
    }

    /** Persona del usuario si ya existe (null si 404 o si no hay usuario). */
    private ThirdPartyClient.PersonResponse findPersonByUser(UUID userId) {
        if (userId == null) return null;
        try {
            return thirdPartyClient.personByUser(userId);
        } catch (FeignException.NotFound e) {
            return null;
        }
    }
}
