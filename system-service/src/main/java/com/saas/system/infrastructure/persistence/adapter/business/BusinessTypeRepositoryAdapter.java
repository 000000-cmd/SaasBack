package com.saas.system.infrastructure.persistence.adapter.business;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.business.BusinessType;
import com.saas.system.domain.port.out.business.IBusinessTypeRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.business.BusinessTypeEntity;
import com.saas.system.infrastructure.persistence.mapper.business.BusinessTypePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.business.JpaBusinessTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class BusinessTypeRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BusinessType, BusinessTypeEntity, UUID>
        implements IBusinessTypeRepositoryPort {

    private final JpaBusinessTypeRepository jpa;

    public BusinessTypeRepositoryAdapter(JpaBusinessTypeRepository jpa,
                                         BusinessTypePersistenceMapper mapper) {
        super(jpa, mapper, "Tipo de negocio");
        this.jpa = jpa;
    }

    @Override
    public Optional<BusinessType> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
