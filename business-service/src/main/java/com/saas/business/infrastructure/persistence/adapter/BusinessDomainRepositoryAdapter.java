package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BusinessDomain;
import com.saas.business.domain.port.out.IBusinessDomainRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BusinessDomainEntity;
import com.saas.business.infrastructure.persistence.mapper.BusinessDomainPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBusinessDomainRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BusinessDomainRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BusinessDomain, BusinessDomainEntity, UUID>
        implements IBusinessDomainRepositoryPort {

    private final JpaBusinessDomainRepository jpa;

    public BusinessDomainRepositoryAdapter(JpaBusinessDomainRepository jpa, BusinessDomainPersistenceMapper mapper) {
        super(jpa, mapper, "Dominio de empresa");
        this.jpa = jpa;
    }

    @Override
    public Optional<BusinessDomain> findBySlug(String slug) {
        return jpa.findBySlug(slug).map(getMapper()::toDomain);
    }

    @Override
    public List<BusinessDomain> findByBusinessId(UUID businessId) {
        return jpa.findByBusinessId(businessId).stream().map(getMapper()::toDomain).toList();
    }
}
