package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.RolePermission;
import com.saas.system.domain.port.out.IRolePermissionRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.PermissionEntity;
import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import com.saas.system.infrastructure.persistence.entity.RolePermissionEntity;
import com.saas.system.infrastructure.persistence.mapper.RolePermissionPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaRolePermissionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class RolePermissionRepositoryAdapter
        extends BaseJpaRepositoryAdapter<RolePermission, RolePermissionEntity, UUID>
        implements IRolePermissionRepositoryPort {

    private final JpaRolePermissionRepository jpa;

    public RolePermissionRepositoryAdapter(JpaRolePermissionRepository jpa,
                                           RolePermissionPersistenceMapper mapper) {
        super(jpa, mapper, "RolePermission");
        this.jpa = jpa;
    }

    @Override
    public List<RolePermission> findByRoleId(UUID roleId) {
        return getMapper().toDomainList(jpa.findByRoleId(roleId));
    }

    @Override
    public Set<String> findPermissionCodesByRoleId(UUID roleId) {
        return jpa.findPermissionCodesByRoleId(roleId);
    }

    @Override
    public boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId) {
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
}
