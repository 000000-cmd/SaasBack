package com.saas.business.application.service;

import com.saas.business.domain.model.BusinessLanding;
import com.saas.business.domain.port.in.IBusinessLandingUseCase;
import com.saas.business.domain.port.out.IBusinessLandingRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Upsert de la landing del negocio. No extiende GenericCrudService porque el
 * agregado se identifica por businessId (1:1), no por su propio id: el dueño
 * "guarda su página", exista o no el registro.
 */
@Service
@RequiredArgsConstructor
public class BusinessLandingService implements IBusinessLandingUseCase {

    private final IBusinessLandingRepositoryPort repo;

    @Override
    @Transactional(readOnly = true)
    public Optional<BusinessLanding> findByBusiness(UUID businessId) {
        return repo.findByBusinessId(businessId);
    }

    @Override
    @Transactional
    public BusinessLanding upsert(UUID businessId, BusinessLanding incoming) {
        return repo.findByBusinessId(businessId)
                .map(existing -> {
                    applyChanges(existing, incoming);
                    return repo.update(existing);
                })
                .orElseGet(() -> {
                    incoming.setBusinessId(businessId);
                    if (incoming.getPublished() == null) incoming.setPublished(false);
                    return repo.save(incoming);
                });
    }

    /**
     * La landing se edita como documento completo (el editor manda todos los
     * campos), así que el merge asigna todo — un campo vaciado por el dueño
     * debe quedar vacío, no conservar el valor previo.
     */
    private void applyChanges(BusinessLanding existing, BusinessLanding incoming) {
        existing.setTagline(incoming.getTagline());
        existing.setAbout(incoming.getAbout());
        existing.setPhone(incoming.getPhone());
        existing.setWhatsapp(incoming.getWhatsapp());
        existing.setContactEmail(incoming.getContactEmail());
        existing.setInstagram(incoming.getInstagram());
        existing.setFacebook(incoming.getFacebook());
        existing.setHeroImageUrl(incoming.getHeroImageUrl());
        existing.setGalleryJson(incoming.getGalleryJson());
        existing.setScheduleText(incoming.getScheduleText());
        if (incoming.getPublished() != null) existing.setPublished(incoming.getPublished());
    }
}
