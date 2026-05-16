package com.saas.system.infrastructure.persistence.adapter.business;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.business.Status;
import com.saas.system.domain.port.out.business.IStatusRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.business.StatusEntity;
import com.saas.system.infrastructure.persistence.mapper.business.StatusPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.business.JpaStatusRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class StatusRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Status, StatusEntity, UUID>
        implements IStatusRepositoryPort {

    private final JpaStatusRepository jpa;

    public StatusRepositoryAdapter(JpaStatusRepository jpa,
                                   StatusPersistenceMapper mapper) {
        super(jpa, mapper, "Estado");
        this.jpa = jpa;
    }

    @Override
    public Optional<Status> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
