package com.saas.finance.application.service;

import com.saas.finance.domain.model.BusinessCompensation;
import com.saas.finance.domain.port.in.IBusinessCompensationUseCase;
import com.saas.finance.domain.port.out.IBusinessCompensationRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BusinessCompensationService extends GenericCrudService<BusinessCompensation, UUID>
        implements IBusinessCompensationUseCase {
    private final IBusinessCompensationRepositoryPort repo;
    public BusinessCompensationService(IBusinessCompensationRepositoryPort repo) { super(repo); this.repo = repo; }
    @Override protected String getResourceName() { return "Compensacion de negocio"; }

    @Override protected void onBeforeCreate(BusinessCompensation e) {
        if (e.getValidFrom() == null) e.setValidFrom(LocalDateTime.now());
        e.setValidTo(null);
    }
    @Override protected void applyChanges(BusinessCompensation e, BusinessCompensation i) {
        if (i.getCompensationType() != null) e.setCompensationType(i.getCompensationType());
        if (i.getCompensationValue() != null) e.setCompensationValue(i.getCompensationValue());
    }

    @Override @Transactional
    public BusinessCompensation supersede(UUID currentId, BusinessCompensation incoming) {
        BusinessCompensation current = getById(currentId);
        LocalDateTime now = LocalDateTime.now();
        current.setValidTo(now);
        repository.update(current);
        incoming.setBusinessId(current.getBusinessId());
        incoming.setValidFrom(now);
        incoming.setValidTo(null);
        return create(incoming);
    }

    @Override @Transactional(readOnly = true)
    public List<BusinessCompensation> findByBusiness(UUID businessId) { return repo.findByBusinessId(businessId); }
    @Override @Transactional(readOnly = true)
    public Optional<BusinessCompensation> findCurrentByBusiness(UUID businessId) {
        return repo.findByBusinessIdAndValidToIsNull(businessId).stream().findFirst();
    }
}
