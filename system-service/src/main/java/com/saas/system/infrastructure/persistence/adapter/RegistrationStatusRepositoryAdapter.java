package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.RegistrationStatus;
import com.saas.system.domain.port.out.IRegistrationStatusRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.RegistrationStatusEntity;
import com.saas.system.infrastructure.persistence.mapper.RegistrationStatusPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaRegistrationStatusRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class RegistrationStatusRepositoryAdapter
        extends BaseJpaRepositoryAdapter<RegistrationStatus, RegistrationStatusEntity, UUID>
        implements IRegistrationStatusRepositoryPort {

    private final JpaRegistrationStatusRepository jpa;

    public RegistrationStatusRepositoryAdapter(JpaRegistrationStatusRepository jpa,
                                               RegistrationStatusPersistenceMapper mapper) {
        super(jpa, mapper, "Estado de registro");
        this.jpa = jpa;
    }

    @Override
    public Optional<RegistrationStatus> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
