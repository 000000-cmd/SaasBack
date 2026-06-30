package com.saas.thirdparty.application.service;

import com.saas.common.exception.DuplicateResourceException;
import com.saas.common.service.GenericCrudService;
import com.saas.thirdparty.domain.model.ThirdParty;
import com.saas.thirdparty.domain.port.in.IThirdPartyUseCase;
import com.saas.thirdparty.domain.port.out.IThirdPartyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de Terceros (persona natural). CRUD generico + publicacion del
 * evento ANIDADO (tercero + contactos + direcciones) via {@link ThirdPartyReindexPublisher}.
 */
@Service
public class ThirdPartyService extends GenericCrudService<ThirdParty, UUID>
        implements IThirdPartyUseCase {

    private final IThirdPartyRepositoryPort thirdPartyRepo;
    private final ThirdPartyReindexPublisher reindexPublisher;

    public ThirdPartyService(IThirdPartyRepositoryPort repo, ThirdPartyReindexPublisher reindexPublisher) {
        super(repo);
        this.thirdPartyRepo = repo;
        this.reindexPublisher = reindexPublisher;
    }

    @Override protected String getResourceName() { return "Tercero"; }

    @Override
    protected void applyChanges(ThirdParty existing, ThirdParty incoming) {
        if (incoming.getDocumentTypeId() != null)  existing.setDocumentTypeId(incoming.getDocumentTypeId());
        if (incoming.getDocumentNumber() != null)  existing.setDocumentNumber(incoming.getDocumentNumber());
        if (incoming.getUserId() != null)          existing.setUserId(incoming.getUserId());
        if (incoming.getFirstName() != null)       existing.setFirstName(incoming.getFirstName());
        if (incoming.getSecondName() != null)      existing.setSecondName(incoming.getSecondName());
        if (incoming.getFirstLastName() != null)   existing.setFirstLastName(incoming.getFirstLastName());
        if (incoming.getSecondLastName() != null)  existing.setSecondLastName(incoming.getSecondLastName());
        if (incoming.getGenderId() != null)        existing.setGenderId(incoming.getGenderId());
        if (incoming.getBirthDate() != null)       existing.setBirthDate(incoming.getBirthDate());
        if (incoming.getPhotoUrl() != null)        existing.setPhotoUrl(incoming.getPhotoUrl());
    }

    @Override
    protected void onBeforeCreate(ThirdParty entity) {
        if (thirdPartyRepo.existsByDocument(entity.getDocumentTypeId(), entity.getDocumentNumber())) {
            throw new DuplicateResourceException(getResourceName(), "documentNumber", entity.getDocumentNumber());
        }
    }

    @Override @Transactional(readOnly = true)
    public Optional<ThirdParty> findByDocument(UUID documentTypeId, String documentNumber) {
        return thirdPartyRepo.findByDocument(documentTypeId, documentNumber);
    }

    @Override @Transactional(readOnly = true)
    public boolean existsByDocument(UUID documentTypeId, String documentNumber) {
        return thirdPartyRepo.existsByDocument(documentTypeId, documentNumber);
    }

    @Override @Transactional(readOnly = true)
    public Optional<ThirdParty> findByUserId(UUID userId) {
        return thirdPartyRepo.findByUserId(userId);
    }

    @Override @Transactional(readOnly = true)
    public java.util.List<ThirdParty> findByIds(java.util.Collection<UUID> ids) {
        return thirdPartyRepo.findByIds(ids);
    }

    @Override protected void onAfterCreate(ThirdParty saved) { reindexPublisher.publishUpsert(saved.getId(), true); }
    @Override protected void onAfterUpdate(ThirdParty existing, ThirdParty updated) { reindexPublisher.publishUpsert(updated.getId(), false); }
    @Override protected void onAfterDelete(UUID id, ThirdParty deletedSnapshot) { reindexPublisher.publishDelete(id, deletedSnapshot); }
}
