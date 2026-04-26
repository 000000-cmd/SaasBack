package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.RolePermission;
import com.saas.system.domain.port.out.IRolePermissionRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.PermissionEntity;
import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import com.saas.system.infrastructure.persistence.entity.RolePermissionEntity;
import com.saas.system.infrastructure.persistence.mapper.RolePermissionPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaRolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RolePermissionRepositoryAdapter implements IRolePermissionRepositoryPort {

    private final JpaRolePermissionRepository jpa;
    private final RolePermissionPersistenceMapper mapper;

    @Override
    public RolePermission save(RolePermission domain) {
        return mapper.toDomain(jpa.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional
    public RolePermission update(RolePermission domain) {
        RolePermissionEntity existing = jpa.findById(domain.getId())
                .orElseThrow(() -> new ResourceNotFoundException("RolePermission", "Id", domain.getId()));
        mapper.updateEntityFromDomain(domain, existing);
        return mapper.toDomain(jpa.save(existing));
    }

    @Override public Optional<RolePermission> findById(UUID id)   { return jpa.findById(id).map(mapper::toDomain); }
    @Override public boolean                  existsById(UUID id) { return jpa.existsById(id); }
    @Override public List<RolePermission>     findAll()           { return mapper.toDomainList(jpa.findAll()); }
    @Override public List<RolePermission>     findByRoleId(UUID roleId) {
        return mapper.toDomainList(jpa.findByRoleId(roleId));
    }
    @Override public Set<String>              findPermissionCodesByRoleId(UUID roleId) {
        return jpa.findPermissionCodesByRoleId(roleId);
    }
    @Override public boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId) {
        return jpa.existsByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Override
    @Transactional
    public void replacePermissionsForRole(UUID roleId, Set<UUID> permissionIds) {
        Set<UUID> desired = permissionIds == null ? Set.of() : new HashSet<>(permissionIds);
        List<RolePermissionEntity> current = jpa.findByRoleId(roleId);
        Set<UUID> currentIds = current.stream()
                .map(rp -> rp.getPermission().getId())
                .collect(Collectors.toSet());

        current.stream()
                .filter(rp -> !desired.contains(rp.getPermission().getId()))
                .forEach(jpa::delete);

        RoleEntity roleRef = new RoleEntity();
        roleRef.setId(roleId);

        desired.stream()
                .filter(pid -> !currentIds.contains(pid))
                .map(pid -> {
                    PermissionEntity p = new PermissionEntity();
                    p.setId(pid);
                    return RolePermissionEntity.builder().role(roleRef).permission(p).build();
                })
                .forEach(jpa::save);
    }

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
