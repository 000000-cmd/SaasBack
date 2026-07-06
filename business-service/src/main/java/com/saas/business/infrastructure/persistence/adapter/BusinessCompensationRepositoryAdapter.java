package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BusinessCompensation;
import com.saas.business.domain.port.out.IBusinessCompensationRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BusinessCompensationEntity;
import com.saas.business.infrastructure.persistence.mapper.BusinessCompensationPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBusinessCompensationRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class BusinessCompensationRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BusinessCompensation, BusinessCompensationEntity, UUID>
        implements IBusinessCompensationRepositoryPort {
    private final JpaBusinessCompensationRepository jpa;
    public BusinessCompensationRepositoryAdapter(JpaBusinessCompensationRepository jpa, BusinessCompensationPersistenceMapper mapper) {
        super(jpa, mapper, "Compensacion de negocio"); this.jpa = jpa;
    }
    @Override public List<BusinessCompensation> findByBusinessId(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessId(businessId));
    }
    @Override public List<BusinessCompensation> findByBusinessIdAndValidToIsNull(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessIdAndValidToIsNull(businessId));
    }
}
