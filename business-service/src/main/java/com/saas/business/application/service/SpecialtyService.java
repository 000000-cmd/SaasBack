package com.saas.business.application.service;

import com.saas.business.domain.model.Specialty;
import com.saas.business.domain.port.in.ISpecialtyUseCase;
import com.saas.business.domain.port.out.ISpecialtyRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class SpecialtyService extends GenericCrudService<Specialty, UUID> implements ISpecialtyUseCase {
    private final ISpecialtyRepositoryPort repo;
    public SpecialtyService(ISpecialtyRepositoryPort repo) { super(repo); this.repo = repo; }
    @Override protected String getResourceName() { return "Especialidad"; }
    @Override protected void applyChanges(Specialty e, Specialty i) {
        if (i.getName() != null) e.setName(i.getName());
        if (i.getDisplayOrder() != null) e.setDisplayOrder(i.getDisplayOrder());
    }
    @Override @Transactional(readOnly = true)
    public List<Specialty> findByBusiness(UUID businessId) { return repo.findByBusinessId(businessId); }
}
