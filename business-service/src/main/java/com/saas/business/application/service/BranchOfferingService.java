package com.saas.business.application.service;

import com.saas.business.domain.model.BranchOffering;
import com.saas.business.domain.port.in.IBranchOfferingUseCase;
import com.saas.business.domain.port.out.IBranchOfferingRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class BranchOfferingService extends GenericCrudService<BranchOffering, UUID> implements IBranchOfferingUseCase {
    private final IBranchOfferingRepositoryPort repo;
    public BranchOfferingService(IBranchOfferingRepositoryPort repo) { super(repo); this.repo = repo; }
    @Override protected String getResourceName() { return "Oferta de sede"; }
    @Override protected void applyChanges(BranchOffering e, BranchOffering i) {
        if (i.getOfferingId() != null) e.setOfferingId(i.getOfferingId());
        if (i.getName() != null) e.setName(i.getName());
        if (i.getDescription() != null) e.setDescription(i.getDescription());
        if (i.getDurationMinutes() != null) e.setDurationMinutes(i.getDurationMinutes());
        if (i.getPrice() != null) e.setPrice(i.getPrice());
        if (i.getIsEnabled() != null) e.setIsEnabled(i.getIsEnabled());
        if (i.getIsActive() != null) e.setIsActive(i.getIsActive());
    }
    @Override @Transactional(readOnly = true)
    public List<BranchOffering> findByBranch(UUID branchId) { return repo.findByBranchId(branchId); }
}
