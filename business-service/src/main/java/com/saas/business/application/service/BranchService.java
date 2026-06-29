package com.saas.business.application.service;

import com.saas.business.domain.model.Branch;
import com.saas.business.domain.port.in.IBranchUseCase;
import com.saas.business.domain.port.out.IBranchRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class BranchService extends GenericCrudService<Branch, UUID> implements IBranchUseCase {

    private final IBranchRepositoryPort repo;
    public BranchService(IBranchRepositoryPort repo) { super(repo); this.repo = repo; }

    @Override protected String getResourceName() { return "Sede"; }

    @Override
    protected void applyChanges(Branch existing, Branch incoming) {
        if (incoming.getBranchTypeId() != null)  existing.setBranchTypeId(incoming.getBranchTypeId());
        if (incoming.getName() != null)          existing.setName(incoming.getName());
        if (incoming.getCode() != null)          existing.setCode(incoming.getCode());
        if (incoming.getMunicipalityId() != null) existing.setMunicipalityId(incoming.getMunicipalityId());
        if (incoming.getNeighborhoodId() != null) existing.setNeighborhoodId(incoming.getNeighborhoodId());
        if (incoming.getAddressLine() != null)   existing.setAddressLine(incoming.getAddressLine());
        if (incoming.getPhone() != null)         existing.setPhone(incoming.getPhone());
        if (incoming.getIsMain() != null)        existing.setIsMain(incoming.getIsMain());
        if (incoming.getStatusId() != null)      existing.setStatusId(incoming.getStatusId());
    }

    @Override @Transactional(readOnly = true)
    public List<Branch> findByBusiness(UUID businessId) { return repo.findByBusinessId(businessId); }
}
