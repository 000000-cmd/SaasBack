package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.Role;
import com.saas.system.domain.port.out.IRoleRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import com.saas.system.infrastructure.persistence.mapper.RolePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaRoleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class RoleRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Role, RoleEntity, UUID>
        implements IRoleRepositoryPort {

    private final JpaRoleRepository jpa;

    public RoleRepositoryAdapter(JpaRoleRepository jpa, RolePersistenceMapper mapper) {
        super(jpa, mapper, "Role");
        this.jpa = jpa;
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }

    @Override
    public List<Role> findAllByIds(Set<UUID> ids) {
        return getMapper().toDomainList(jpa.findByIdIn(ids));
    }
}
