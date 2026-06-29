package com.saas.thirdparty.application.service;

import com.saas.common.service.GenericCrudService;
import com.saas.thirdparty.domain.model.ThirdPartyContact;
import com.saas.thirdparty.domain.port.in.IThirdPartyContactUseCase;
import com.saas.thirdparty.domain.port.out.IThirdPartyContactRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ThirdPartyContactService extends GenericCrudService<ThirdPartyContact, UUID>
        implements IThirdPartyContactUseCase {

    private final IThirdPartyContactRepositoryPort repo;
    private final ThirdPartyReindexPublisher reindexPublisher;

    public ThirdPartyContactService(IThirdPartyContactRepositoryPort repo, ThirdPartyReindexPublisher reindexPublisher) {
        super(repo);
        this.repo = repo;
        this.reindexPublisher = reindexPublisher;
    }

    @Override protected String getResourceName() { return "Contacto de tercero"; }

    /**
     * Al crear desde el admin, el medio NO se verifica (eso lo hace cada usuario
     * con su propio flujo). Coercionamos los flags a false si vienen nulos para
     * respetar las columnas NOT NULL (IsVerified / IsPrimary).
     */
    @Override
    protected void onBeforeCreate(ThirdPartyContact contact) {
        if (contact.getIsVerified() == null) contact.setIsVerified(Boolean.FALSE);
        if (contact.getIsPrimary() == null)  contact.setIsPrimary(Boolean.FALSE);
    }

    @Override
    protected void applyChanges(ThirdPartyContact existing, ThirdPartyContact incoming) {
        if (incoming.getContactTypeId() != null) existing.setContactTypeId(incoming.getContactTypeId());
        if (incoming.getValue() != null)         existing.setValue(incoming.getValue());
        if (incoming.getIsPrimary() != null)     existing.setIsPrimary(incoming.getIsPrimary());
        if (incoming.getIsVerified() != null)    existing.setIsVerified(incoming.getIsVerified());
        if (incoming.getNotes() != null)         existing.setNotes(incoming.getNotes());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThirdPartyContact> findByThirdParty(UUID thirdPartyId) {
        return repo.findByThirdPartyId(thirdPartyId);
    }

    @Override protected void onAfterCreate(com.saas.thirdparty.domain.model.ThirdPartyContact saved) { reindexPublisher.reindex(saved.getThirdPartyId()); }
    @Override protected void onAfterUpdate(com.saas.thirdparty.domain.model.ThirdPartyContact existing, com.saas.thirdparty.domain.model.ThirdPartyContact updated) { reindexPublisher.reindex(updated.getThirdPartyId()); }
    @Override protected void onAfterDelete(java.util.UUID id, com.saas.thirdparty.domain.model.ThirdPartyContact snapshot) { reindexPublisher.reindex(snapshot.getThirdPartyId()); }
}
