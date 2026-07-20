package com.saas.finance.application.service;

import com.saas.finance.domain.model.BranchCompensation;
import com.saas.finance.domain.port.in.IBranchCompensationUseCase;
import com.saas.finance.domain.port.out.IBranchCompensationRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BranchCompensationService extends GenericCrudService<BranchCompensation, UUID>
        implements IBranchCompensationUseCase {
    private final IBranchCompensationRepositoryPort repo;
    public BranchCompensationService(IBranchCompensationRepositoryPort repo) { super(repo); this.repo = repo; }
    @Override protected String getResourceName() { return "Compensacion de sede"; }

    @Override protected void onBeforeCreate(BranchCompensation e) {
        if (e.getValidFrom() == null) e.setValidFrom(LocalDateTime.now());
        e.setValidTo(null);
    }
    @Override protected void applyChanges(BranchCompensation e, BranchCompensation i) {
        if (i.getCompensationType() != null) e.setCompensationType(i.getCompensationType());
        if (i.getCompensationValue() != null) e.setCompensationValue(i.getCompensationValue());
        e.setSalaryBase(i.getSalaryBase()); // nullable a proposito: al cambiar a un tipo no-hibrido se limpia
    }

    @Override @Transactional
    public BranchCompensation supersede(UUID currentId, BranchCompensation incoming) {
        BranchCompensation current = getById(currentId);
        LocalDateTime now = LocalDateTime.now();
        current.setValidTo(now);
        repository.update(current);
        incoming.setBranchId(current.getBranchId());
        incoming.setValidFrom(now);
        incoming.setValidTo(null);
        return create(incoming);
    }

    @Override @Transactional(readOnly = true)
    public List<BranchCompensation> findByBranch(UUID branchId) { return repo.findByBranchId(branchId); }
    @Override @Transactional(readOnly = true)
    public Optional<BranchCompensation> findCurrentByBranch(UUID branchId) {
        return repo.findByBranchIdAndValidToIsNull(branchId).stream().findFirst();
    }
}
