package com.saas.business.application.service;

import com.saas.business.domain.model.BusinessScheduleShift;
import com.saas.business.domain.port.in.IBusinessScheduleShiftUseCase;
import com.saas.business.domain.port.out.IBusinessScheduleShiftRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class BusinessScheduleShiftService extends GenericCrudService<BusinessScheduleShift, UUID> implements IBusinessScheduleShiftUseCase {
    private final IBusinessScheduleShiftRepositoryPort repo;
    public BusinessScheduleShiftService(IBusinessScheduleShiftRepositoryPort repo) { super(repo); this.repo = repo; }
    @Override protected String getResourceName() { return "Turno de horario de empresa"; }
    @Override protected void applyChanges(BusinessScheduleShift e, BusinessScheduleShift i) {
        if (i.getShiftTypeId() != null) e.setShiftTypeId(i.getShiftTypeId());
        if (i.getDayOfWeekId() != null) e.setDayOfWeekId(i.getDayOfWeekId());
        if (i.getStartTime() != null) e.setStartTime(i.getStartTime());
        if (i.getEndTime() != null) e.setEndTime(i.getEndTime());
        if (i.getDisplayOrder() != null) e.setDisplayOrder(i.getDisplayOrder());
    }
    @Override @Transactional(readOnly = true)
    public List<BusinessScheduleShift> findByBusinessSchedule(UUID businessScheduleId) { return repo.findByBusinessScheduleId(businessScheduleId); }
}
