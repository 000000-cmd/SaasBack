package com.saas.business.application.service;

import com.saas.business.domain.model.OfferingCategory;
import com.saas.business.domain.port.in.IOfferingCategoryUseCase;
import com.saas.business.domain.port.out.IOfferingCategoryRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class OfferingCategoryService extends GenericCrudService<OfferingCategory, UUID> implements IOfferingCategoryUseCase {
    private final IOfferingCategoryRepositoryPort repo;
    public OfferingCategoryService(IOfferingCategoryRepositoryPort repo) { super(repo); this.repo = repo; }
    @Override protected String getResourceName() { return "Categoria de oferta"; }
    @Override protected void applyChanges(OfferingCategory e, OfferingCategory i) {
        if (i.getName() != null) e.setName(i.getName());
        if (i.getDisplayOrder() != null) e.setDisplayOrder(i.getDisplayOrder());
    }
    @Override @Transactional(readOnly = true)
    public List<OfferingCategory> findByBusiness(UUID businessId) { return repo.findByBusinessId(businessId); }
}
