package com.saas.business.application.service;

import com.saas.business.domain.model.BusinessSchedule;
import com.saas.business.domain.port.in.IBusinessScheduleUseCase;
import com.saas.business.domain.port.out.IBusinessScheduleRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BusinessScheduleService extends GenericCrudService<BusinessSchedule, UUID> implements IBusinessScheduleUseCase {
    private final IBusinessScheduleRepositoryPort repo;
    public BusinessScheduleService(IBusinessScheduleRepositoryPort repo) { super(repo); this.repo = repo; }
    @Override protected String getResourceName() { return "Horario de empresa"; }

    @Override protected void onBeforeCreate(BusinessSchedule e) {
        if (e.getValidFrom() == null) e.setValidFrom(LocalDateTime.now());
        e.setValidTo(null);
    }
    @Override protected void applyChanges(BusinessSchedule e, BusinessSchedule i) {
        if (i.getScheduleTypeId() != null) e.setScheduleTypeId(i.getScheduleTypeId());
        if (i.getName() != null) e.setName(i.getName());
        if (i.getValidFrom() != null) e.setValidFrom(i.getValidFrom());
        if (i.getValidTo() != null) e.setValidTo(i.getValidTo());
    }

    @Override @Transactional
    public BusinessSchedule supersede(UUID currentId, BusinessSchedule incoming) {
        BusinessSchedule current = getById(currentId);
        LocalDateTime now = LocalDateTime.now();
        current.setValidTo(now);
        repository.update(current);
        incoming.setBusinessId(current.getBusinessId());
        incoming.setValidFrom(now);
        incoming.setValidTo(null);
        return create(incoming);
    }

    @Override @Transactional(readOnly = true)
    public List<BusinessSchedule> findByBusiness(UUID businessId) { return repo.findByBusinessId(businessId); }
    @Override @Transactional(readOnly = true)
    public List<BusinessSchedule> findCurrentByBusiness(UUID businessId) { return repo.findByBusinessIdAndValidToIsNull(businessId); }
}
