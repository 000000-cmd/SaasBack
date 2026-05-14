package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.Gender;
import com.saas.system.domain.port.out.IGenderRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.GenderEntity;
import com.saas.system.infrastructure.persistence.mapper.GenderPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaGenderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class GenderRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Gender, GenderEntity, UUID>
        implements IGenderRepositoryPort {

    private final JpaGenderRepository jpa;

    public GenderRepositoryAdapter(JpaGenderRepository jpa,
                                   GenderPersistenceMapper mapper) {
        super(jpa, mapper, "Genero");
        this.jpa = jpa;
    }

    @Override
    public Optional<Gender> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
