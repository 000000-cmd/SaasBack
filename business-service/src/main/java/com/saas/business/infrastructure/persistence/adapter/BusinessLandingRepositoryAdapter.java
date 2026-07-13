package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BusinessLanding;
import com.saas.business.domain.port.out.IBusinessLandingRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BusinessLandingEntity;
import com.saas.business.infrastructure.persistence.mapper.BusinessLandingPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBusinessLandingRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class BusinessLandingRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BusinessLanding, BusinessLandingEntity, UUID>
        implements IBusinessLandingRepositoryPort {

    private final JpaBusinessLandingRepository jpa;

    public BusinessLandingRepositoryAdapter(JpaBusinessLandingRepository jpa, BusinessLandingPersistenceMapper mapper) {
        super(jpa, mapper, "Landing de empresa");
        this.jpa = jpa;
    }

    @Override
    public Optional<BusinessLanding> findByBusinessId(UUID businessId) {
        return jpa.findByBusinessId(businessId).map(getMapper()::toDomain);
    }
}
