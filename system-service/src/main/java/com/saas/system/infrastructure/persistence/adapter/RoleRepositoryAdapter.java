package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.Role;
import com.saas.system.domain.port.out.IRoleRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import com.saas.system.infrastructure.persistence.mapper.RolePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements IRoleRepositoryPort {

    private final JpaRoleRepository jpa;
    private final RolePersistenceMapper mapper;

    @Override
    public Role save(Role domain) {
        return mapper.toDomain(jpa.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional
    public Role update(Role domain) {
        RoleEntity existing = jpa.findById(domain.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "Id", domain.getId()));
        mapper.updateEntityFromDomain(domain, existing);
        return mapper.toDomain(jpa.save(existing));
    }

    @Override public Optional<Role> findById(UUID id)            { return jpa.findById(id).map(mapper::toDomain); }
    @Override public boolean      existsById(UUID id)            { return jpa.existsById(id); }
    @Override public List<Role>   findAll()                       { return mapper.toDomainList(jpa.findAll()); }
    @Override public Optional<Role> findByCode(String code)      { return jpa.findByCode(code).map(mapper::toDomain); }
    @Override public boolean      existsByCode(String code)      { return jpa.existsByCode(code); }
    @Override public List<Role>   findAllByIds(Set<UUID> ids)    { return mapper.toDomainList(jpa.findByIdIn(ids)); }

    @Override
    @Transactional
    public void softDeleteById(UUID id) {
        jpa.findById(id).ifPresent(e -> {
            e.setEnabled(false);
            e.setVisible(false);
            jpa.save(e);
        });
    }

    @Override public void hardDeleteById(UUID id) { jpa.deleteById(id); }
}
