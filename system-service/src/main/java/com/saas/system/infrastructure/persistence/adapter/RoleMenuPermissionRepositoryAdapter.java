package com.saas.system.infrastructure.persistence.adapter;

import com.saas.system.domain.model.RoleMenuPermission;
import com.saas.system.domain.port.out.IRoleMenuPermissionRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.PermissionEntity;
import com.saas.system.infrastructure.persistence.entity.RoleMenuEntity;
import com.saas.system.infrastructure.persistence.entity.RoleMenuPermissionEntity;
import com.saas.system.infrastructure.persistence.mapper.RoleMenuPermissionPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaPermissionRepository;
import com.saas.system.infrastructure.persistence.repository.JpaRoleMenuPermissionRepository;
import com.saas.system.infrastructure.persistence.repository.JpaRoleMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para permisos de RoleMenu.
 */
@Repository
@RequiredArgsConstructor
public class RoleMenuPermissionRepositoryAdapter implements IRoleMenuPermissionRepositoryPort {

    private final JpaRoleMenuPermissionRepository jpaRepository;
    private final JpaRoleMenuRepository roleMenuRepository;
    private final JpaPermissionRepository permissionRepository;
    private final RoleMenuPermissionPersistenceMapper mapper;

    @Override
    public RoleMenuPermission save(RoleMenuPermission domain) {
        RoleMenuPermissionEntity entity = new RoleMenuPermissionEntity();

        // Obtener las entidades relacionadas
        RoleMenuEntity roleMenu = roleMenuRepository.findById(UUID.fromString(domain.getRoleMenuId()))
                .orElseThrow(() -> new IllegalArgumentException("RoleMenu not found: " + domain.getRoleMenuId()));
        PermissionEntity permission = permissionRepository.findById(UUID.fromString(domain.getPermissionId()))
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + domain.getPermissionId()));

        entity.setRoleMenu(roleMenu);
        entity.setPermission(permission);
        entity.setEnabled(domain.getEnabled());
        entity.setVisible(domain.getVisible());
        entity.setAuditUser(domain.getAuditUser());
        entity.setAuditDate(domain.getAuditDate());

        RoleMenuPermissionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RoleMenuPermission> findById(String id) {
        if (id == null) return Optional.empty();
        return jpaRepository.findById(UUID.fromString(id))
                .map(mapper::toDomain);
    }

    @Override
    public List<RoleMenuPermission> findByRoleMenuId(String roleMenuId) {
        if (roleMenuId == null) return List.of();
        return jpaRepository.findByRoleMenuId(UUID.fromString(roleMenuId)).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        if (id != null) {
            jpaRepository.deleteById(UUID.fromString(id));
        }
    }

    @Override
    public void deleteByRoleMenuId(String roleMenuId) {
        if (roleMenuId != null) {
            jpaRepository.deleteByRoleMenuId(UUID.fromString(roleMenuId));
        }
    }

    @Override
    public void deleteByRoleMenuIdAndPermissionId(String roleMenuId, String permissionId) {
        if (roleMenuId != null && permissionId != null) {
            jpaRepository.deleteByRoleMenuIdAndPermissionId(
                    UUID.fromString(roleMenuId), UUID.fromString(permissionId));
        }
    }

    @Override
    public boolean existsByRoleMenuIdAndPermissionId(String roleMenuId, String permissionId) {
        if (roleMenuId == null || permissionId == null) return false;
        return jpaRepository.existsByRoleMenuIdAndPermissionId(
                UUID.fromString(roleMenuId), UUID.fromString(permissionId));
    }
}
