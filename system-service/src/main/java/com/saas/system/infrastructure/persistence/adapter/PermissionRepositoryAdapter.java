package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.Permission;
import com.saas.system.domain.port.out.IPermissionRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.PermissionEntity;
import com.saas.system.infrastructure.persistence.mapper.PermissionPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaPermissionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PermissionRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Permission, PermissionEntity, UUID>
        implements IPermissionRepositoryPort {

    private final JpaPermissionRepository jpa;

    public PermissionRepositoryAdapter(JpaPermissionRepository jpa,
                                       PermissionPersistenceMapper mapper) {
        super(jpa, mapper, "Permission");
        this.jpa = jpa;
    }

    @Override
    public Optional<Permission> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
