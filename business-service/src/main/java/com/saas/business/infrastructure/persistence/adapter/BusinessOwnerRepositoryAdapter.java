package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BusinessOwner;
import com.saas.business.domain.port.out.IBusinessOwnerRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BusinessOwnerEntity;
import com.saas.business.infrastructure.persistence.mapper.BusinessOwnerPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBusinessOwnerRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class BusinessOwnerRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BusinessOwner, BusinessOwnerEntity, UUID>
        implements IBusinessOwnerRepositoryPort {
    private final JpaBusinessOwnerRepository jpa;
    public BusinessOwnerRepositoryAdapter(JpaBusinessOwnerRepository jpa, BusinessOwnerPersistenceMapper mapper) {
        super(jpa, mapper, "Propietario"); this.jpa = jpa;
    }
    @Override public List<BusinessOwner> findByBusinessId(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessId(businessId));
    }
    @Override public List<BusinessOwner> findByThirdPartyId(UUID thirdPartyId) {
        return getMapper().toDomainList(jpa.findByThirdPartyId(thirdPartyId));
    }
}
