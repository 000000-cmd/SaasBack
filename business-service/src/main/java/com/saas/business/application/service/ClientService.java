package com.saas.business.application.service;

import com.saas.business.domain.model.Client;
import com.saas.business.domain.port.in.IClientUseCase;
import com.saas.business.domain.port.out.IClientRepositoryPort;
import com.saas.common.exception.DuplicateResourceException;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClientService extends GenericCrudService<Client, UUID> implements IClientUseCase {

    private final IClientRepositoryPort repo;
    public ClientService(IClientRepositoryPort repo) { super(repo); this.repo = repo; }

    @Override protected String getResourceName() { return "Cliente"; }

    @Override
    protected void onBeforeCreate(Client entity) {
        if (repo.existsByThirdPartyId(entity.getThirdPartyId())) {
            throw new DuplicateResourceException(getResourceName(), "thirdPartyId", entity.getThirdPartyId());
        }
    }

    @Override
    protected void applyChanges(Client existing, Client incoming) {
        if (incoming.getRegistrationStatusId() != null) existing.setRegistrationStatusId(incoming.getRegistrationStatusId());
        if (incoming.getAcquisitionSource() != null)    existing.setAcquisitionSource(incoming.getAcquisitionSource());
        if (incoming.getNotes() != null)                existing.setNotes(incoming.getNotes());
    }

    @Override @Transactional(readOnly = true)
    public Optional<Client> findByThirdParty(UUID thirdPartyId) { return repo.findByThirdPartyId(thirdPartyId); }
}
