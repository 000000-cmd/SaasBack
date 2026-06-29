package com.saas.thirdparty.application.service;

import com.saas.common.service.GenericCrudService;
import com.saas.thirdparty.domain.model.ThirdPartyAddress;
import com.saas.thirdparty.domain.port.in.IThirdPartyAddressUseCase;
import com.saas.thirdparty.domain.port.out.IThirdPartyAddressRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ThirdPartyAddressService extends GenericCrudService<ThirdPartyAddress, UUID>
        implements IThirdPartyAddressUseCase {

    private final IThirdPartyAddressRepositoryPort repo;
    private final ThirdPartyReindexPublisher reindexPublisher;

    public ThirdPartyAddressService(IThirdPartyAddressRepositoryPort repo, ThirdPartyReindexPublisher reindexPublisher) {
        super(repo);
        this.repo = repo;
        this.reindexPublisher = reindexPublisher;
    }

    @Override protected String getResourceName() { return "Direccion de tercero"; }

    /** Coerciona el flag a false si viene nulo (columna IsPrimary NOT NULL). */
    @Override
    protected void onBeforeCreate(ThirdPartyAddress address) {
        if (address.getIsPrimary() == null) address.setIsPrimary(Boolean.FALSE);
    }

    @Override
    protected void applyChanges(ThirdPartyAddress existing, ThirdPartyAddress incoming) {
        if (incoming.getAddressTypeId() != null)  existing.setAddressTypeId(incoming.getAddressTypeId());
        if (incoming.getMunicipalityId() != null) existing.setMunicipalityId(incoming.getMunicipalityId());
        if (incoming.getNeighborhoodId() != null) existing.setNeighborhoodId(incoming.getNeighborhoodId());
        if (incoming.getLine() != null)           existing.setLine(incoming.getLine());
        if (incoming.getReference() != null)      existing.setReference(incoming.getReference());
        if (incoming.getIsPrimary() != null)      existing.setIsPrimary(incoming.getIsPrimary());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThirdPartyAddress> findByThirdParty(UUID thirdPartyId) {
        return repo.findByThirdPartyId(thirdPartyId);
    }

    @Override protected void onAfterCreate(com.saas.thirdparty.domain.model.ThirdPartyAddress saved) { reindexPublisher.reindex(saved.getThirdPartyId()); }
    @Override protected void onAfterUpdate(com.saas.thirdparty.domain.model.ThirdPartyAddress existing, com.saas.thirdparty.domain.model.ThirdPartyAddress updated) { reindexPublisher.reindex(updated.getThirdPartyId()); }
    @Override protected void onAfterDelete(java.util.UUID id, com.saas.thirdparty.domain.model.ThirdPartyAddress snapshot) { reindexPublisher.reindex(snapshot.getThirdPartyId()); }
}
