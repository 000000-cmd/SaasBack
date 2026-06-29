package com.saas.business.application.service;

import com.saas.business.domain.model.Offering;
import com.saas.business.domain.port.in.IOfferingUseCase;
import com.saas.business.domain.port.out.IOfferingRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class OfferingService extends GenericCrudService<Offering, UUID> implements IOfferingUseCase {
    private final IOfferingRepositoryPort repo;
    public OfferingService(IOfferingRepositoryPort repo) { super(repo); this.repo = repo; }
    @Override protected String getResourceName() { return "Oferta"; }
    @Override protected void applyChanges(Offering e, Offering i) {
        if (i.getCategoryId() != null) e.setCategoryId(i.getCategoryId());
        if (i.getName() != null) e.setName(i.getName());
        if (i.getDescription() != null) e.setDescription(i.getDescription());
        if (i.getDurationMinutes() != null) e.setDurationMinutes(i.getDurationMinutes());
        if (i.getPrice() != null) e.setPrice(i.getPrice());
        if (i.getIsActive() != null) e.setIsActive(i.getIsActive());
    }
    @Override @Transactional(readOnly = true)
    public List<Offering> findByBusiness(UUID businessId) { return repo.findByBusinessId(businessId); }
}
