package com.saas.business.application.service;

import com.saas.business.domain.model.BranchScheduleShift;
import com.saas.business.domain.port.in.IBranchScheduleShiftUseCase;
import com.saas.business.domain.port.out.IBranchScheduleShiftRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class BranchScheduleShiftService extends GenericCrudService<BranchScheduleShift, UUID> implements IBranchScheduleShiftUseCase {
    private final IBranchScheduleShiftRepositoryPort repo;
    public BranchScheduleShiftService(IBranchScheduleShiftRepositoryPort repo) { super(repo); this.repo = repo; }
    @Override protected String getResourceName() { return "Turno de horario de sede"; }
    @Override protected void applyChanges(BranchScheduleShift e, BranchScheduleShift i) {
        if (i.getShiftTypeId() != null) e.setShiftTypeId(i.getShiftTypeId());
        if (i.getDayOfWeekId() != null) e.setDayOfWeekId(i.getDayOfWeekId());
        if (i.getStartTime() != null) e.setStartTime(i.getStartTime());
        if (i.getEndTime() != null) e.setEndTime(i.getEndTime());
        if (i.getDisplayOrder() != null) e.setDisplayOrder(i.getDisplayOrder());
    }
    @Override @Transactional(readOnly = true)
    public List<BranchScheduleShift> findByBranchSchedule(UUID branchScheduleId) { return repo.findByBranchScheduleId(branchScheduleId); }
}
