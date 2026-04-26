package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.Permission;
import com.saas.system.domain.port.out.IPermissionRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.PermissionEntity;
import com.saas.system.infrastructure.persistence.mapper.PermissionPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements IPermissionRepositoryPort {

    private final JpaPermissionRepository jpa;
    private final PermissionPersistenceMapper mapper;

    @Override
    public Permission save(Permission domain) {
        return mapper.toDomain(jpa.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional
    public Permission update(Permission domain) {
        PermissionEntity existing = jpa.findById(domain.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "Id", domain.getId()));
        mapper.updateEntityFromDomain(domain, existing);
        return mapper.toDomain(jpa.save(existing));
    }

    @Override public Optional<Permission> findById(UUID id)       { return jpa.findById(id).map(mapper::toDomain); }
    @Override public boolean              existsById(UUID id)     { return jpa.existsById(id); }
    @Override public List<Permission>     findAll()                { return mapper.toDomainList(jpa.findAll()); }
    @Override public Optional<Permission> findByCode(String code) { return jpa.findByCode(code).map(mapper::toDomain); }
    @Override public boolean              existsByCode(String code) { return jpa.existsByCode(code); }

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
