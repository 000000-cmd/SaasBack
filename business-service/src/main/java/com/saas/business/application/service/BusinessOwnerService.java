package com.saas.business.application.service;

import com.saas.business.domain.model.BusinessOwner;
import com.saas.business.domain.port.in.IBusinessOwnerUseCase;
import com.saas.business.domain.port.out.IBusinessOwnerRepositoryPort;
import com.saas.common.exception.BusinessException;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class BusinessOwnerService extends GenericCrudService<BusinessOwner, UUID> implements IBusinessOwnerUseCase {

    private final IBusinessOwnerRepositoryPort repo;
    public BusinessOwnerService(IBusinessOwnerRepositoryPort repo) { super(repo); this.repo = repo; }

    @Override protected String getResourceName() { return "Propietario"; }

    @Override
    protected void applyChanges(BusinessOwner existing, BusinessOwner incoming) {
        if (incoming.getOwnershipPercentage() != null) existing.setOwnershipPercentage(incoming.getOwnershipPercentage());
        if (incoming.getStartDate() != null) existing.setStartDate(incoming.getStartDate());
        if (incoming.getEndDate() != null)   existing.setEndDate(incoming.getEndDate());
    }

    /** Valida que la suma de % vigentes por empresa no supere 100. */
    @Override
    protected void onBeforeCreate(BusinessOwner entity) {
        validateTotal(entity.getBusinessId(), entity.getOwnershipPercentage(), null);
    }

    private void validateTotal(UUID businessId, BigDecimal incoming, UUID excludeId) {
        BigDecimal sum = repo.findByBusinessId(businessId).stream()
                .filter(o -> o.getEndDate() == null && (excludeId == null || !o.getId().equals(excludeId)))
                .map(BusinessOwner::getOwnershipPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(incoming == null ? BigDecimal.ZERO : incoming);
        if (sum.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("La suma de participacion de los propietarios vigentes supera 100%");
        }
    }

    @Override @Transactional(readOnly = true)
    public List<BusinessOwner> findByBusiness(UUID businessId) { return repo.findByBusinessId(businessId); }
    @Override @Transactional(readOnly = true)
    public List<BusinessOwner> findByThirdParty(UUID thirdPartyId) { return repo.findByThirdPartyId(thirdPartyId); }
}
