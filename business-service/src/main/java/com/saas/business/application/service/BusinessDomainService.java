package com.saas.business.application.service;

import com.saas.business.domain.model.BusinessDomain;
import com.saas.business.domain.port.in.IBusinessDomainUseCase;
import com.saas.business.domain.port.out.IBusinessDomainRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BusinessDomainService extends GenericCrudService<BusinessDomain, UUID>
        implements IBusinessDomainUseCase {

    private final IBusinessDomainRepositoryPort repo;

    public BusinessDomainService(IBusinessDomainRepositoryPort repo) {
        super(repo);
        this.repo = repo;
    }

    @Override protected String getResourceName() { return "Dominio de empresa"; }

    @Override
    protected void onBeforeCreate(BusinessDomain entity) {
        if (entity.getIsPrimary() == null)  entity.setIsPrimary(false);
        if (entity.getIsVerified() == null) entity.setIsVerified(false);
    }

    @Override
    protected void applyChanges(BusinessDomain existing, BusinessDomain incoming) {
        if (incoming.getSlug() != null)         existing.setSlug(incoming.getSlug());
        if (incoming.getCustomDomain() != null) existing.setCustomDomain(incoming.getCustomDomain());
        if (incoming.getIsPrimary() != null)    existing.setIsPrimary(incoming.getIsPrimary());
        if (incoming.getStatusId() != null)     existing.setStatusId(incoming.getStatusId());
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<BusinessDomain> findBySlug(String slug) {
        return repo.findBySlug(slug);
    }

    @Transactional(readOnly = true)
    @Override
    public List<BusinessDomain> findByBusinessId(UUID businessId) {
        return repo.findByBusinessId(businessId);
    }
}
