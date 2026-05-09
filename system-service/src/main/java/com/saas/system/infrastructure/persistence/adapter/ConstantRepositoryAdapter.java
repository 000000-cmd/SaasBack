package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.Constant;
import com.saas.system.domain.port.out.IConstantRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.ConstantEntity;
import com.saas.system.infrastructure.persistence.mapper.ConstantPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaConstantRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ConstantRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Constant, ConstantEntity, UUID>
        implements IConstantRepositoryPort {

    private final JpaConstantRepository jpa;

    public ConstantRepositoryAdapter(JpaConstantRepository jpa,
                                     ConstantPersistenceMapper mapper) {
        super(jpa, mapper, "Constant");
        this.jpa = jpa;
    }

    @Override
    public Optional<Constant> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
