package com.saas.business.application.service;

import com.saas.business.domain.model.BranchSchedule;
import com.saas.business.domain.port.in.IBranchScheduleUseCase;
import com.saas.business.domain.port.out.IBranchScheduleRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BranchScheduleService extends GenericCrudService<BranchSchedule, UUID> implements IBranchScheduleUseCase {
    private final IBranchScheduleRepositoryPort repo;
    public BranchScheduleService(IBranchScheduleRepositoryPort repo) { super(repo); this.repo = repo; }
    @Override protected String getResourceName() { return "Horario de sede"; }

    @Override protected void onBeforeCreate(BranchSchedule e) {
        if (e.getValidFrom() == null) e.setValidFrom(LocalDateTime.now());
        e.setValidTo(null);
    }
    @Override protected void applyChanges(BranchSchedule e, BranchSchedule i) {
        if (i.getBusinessScheduleId() != null) e.setBusinessScheduleId(i.getBusinessScheduleId());
        if (i.getScheduleTypeId() != null) e.setScheduleTypeId(i.getScheduleTypeId());
        if (i.getName() != null) e.setName(i.getName());
        if (i.getValidFrom() != null) e.setValidFrom(i.getValidFrom());
        if (i.getValidTo() != null) e.setValidTo(i.getValidTo());
    }

    @Override @Transactional
    public BranchSchedule supersede(UUID currentId, BranchSchedule incoming) {
        BranchSchedule current = getById(currentId);
        LocalDateTime now = LocalDateTime.now();
        current.setValidTo(now);
        repository.update(current);
        incoming.setBranchId(current.getBranchId());
        incoming.setValidFrom(now);
        incoming.setValidTo(null);
        return create(incoming);
    }

    @Override @Transactional(readOnly = true)
    public List<BranchSchedule> findByBranch(UUID branchId) { return repo.findByBranchId(branchId); }
    @Override @Transactional(readOnly = true)
    public List<BranchSchedule> findCurrentByBranch(UUID branchId) { return repo.findByBranchIdAndValidToIsNull(branchId); }
}
