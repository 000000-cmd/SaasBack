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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

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

        ThirdPartyClient.PersonResponse person = thirdPartyClient.createPerson(
                new ThirdPartyClient.CreatePersonRequest(
                        r.ownerDocumentTypeId(), r.ownerDocumentNumber(), r.ownerUserId(), business.getId(),
                        r.ownerFirstName(), r.ownerSecondName(), r.ownerFirstLastName(), r.ownerSecondLastName(),
                        r.ownerGenderId(), r.ownerBirthDate(), null));

        BusinessOwner owner = ownerUseCase.create(BusinessOwner.builder()
                .businessId(business.getId())
                .thirdPartyId(person.id())
                .ownershipPercentage(new BigDecimal("100"))
                .startDate(LocalDate.now())
                .build());

        return new ProvisionResponse(business.getId(), domain.getSlug(), person.id(), owner.getId());
    }
}
