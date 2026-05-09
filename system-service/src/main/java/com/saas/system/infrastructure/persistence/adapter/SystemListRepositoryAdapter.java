package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.SystemList;
import com.saas.system.domain.port.out.ISystemListRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.SystemListEntity;
import com.saas.system.infrastructure.persistence.mapper.SystemListPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaSystemListRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class SystemListRepositoryAdapter
        extends BaseJpaRepositoryAdapter<SystemList, SystemListEntity, UUID>
        implements ISystemListRepositoryPort {

    private final JpaSystemListRepository jpa;

    public SystemListRepositoryAdapter(JpaSystemListRepository jpa,
                                       SystemListPersistenceMapper mapper) {
        super(jpa, mapper, "SystemList");
        this.jpa = jpa;
    }

    @Override
    public Optional<SystemList> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
