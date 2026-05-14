package com.saas.system.infrastructure.persistence.adapter.business;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.business.BranchType;
import com.saas.system.domain.port.out.business.IBranchTypeRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.business.BranchTypeEntity;
import com.saas.system.infrastructure.persistence.mapper.business.BranchTypePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.business.JpaBranchTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class BranchTypeRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BranchType, BranchTypeEntity, UUID>
        implements IBranchTypeRepositoryPort {

    private final JpaBranchTypeRepository jpa;

    public BranchTypeRepositoryAdapter(JpaBranchTypeRepository jpa,
                                       BranchTypePersistenceMapper mapper) {
        super(jpa, mapper, "Tipo de sucursal");
        this.jpa = jpa;
    }

    @Override
    public Optional<BranchType> findByCode(String code) {
        return jpa.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
