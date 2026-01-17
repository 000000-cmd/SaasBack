package com.saas.system.infrastructure.persistence.adapter;

import com.saas.system.domain.model.RoleMenu;
import com.saas.system.domain.port.out.IRoleMenuRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.MenuEntity;
import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import com.saas.system.infrastructure.persistence.entity.RoleMenuEntity;
import com.saas.system.infrastructure.persistence.mapper.RoleMenuPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaMenuRepository;
import com.saas.system.infrastructure.persistence.repository.JpaRoleMenuRepository;
import com.saas.system.infrastructure.persistence.repository.JpaRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para asignaciones RoleMenu.
 */
@Repository
@RequiredArgsConstructor
public class RoleMenuRepositoryAdapter implements IRoleMenuRepositoryPort {

    private final JpaRoleMenuRepository jpaRepository;
    private final JpaRoleRepository roleRepository;
    private final JpaMenuRepository menuRepository;
    private final RoleMenuPersistenceMapper mapper;

    @Override
    public RoleMenu save(RoleMenu domain) {
        RoleMenuEntity entity = new RoleMenuEntity();

        // Obtener las entidades relacionadas
        RoleEntity role = roleRepository.findById(UUID.fromString(domain.getRoleId()))
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + domain.getRoleId()));
        MenuEntity menu = menuRepository.findById(UUID.fromString(domain.getMenuId()))
                .orElseThrow(() -> new IllegalArgumentException("Menu not found: " + domain.getMenuId()));

        entity.setRole(role);
        entity.setMenu(menu);
        entity.setEnabled(domain.getEnabled());
        entity.setVisible(domain.getVisible());
        entity.setAuditUser(domain.getAuditUser());
        entity.setAuditDate(domain.getAuditDate());

        RoleMenuEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RoleMenu> findById(String id) {
        if (id == null) return Optional.empty();
        return jpaRepository.findById(UUID.fromString(id))
                .map(mapper::toDomain);
    }

    @Override
    public List<RoleMenu> findByRoleId(String roleId) {
        if (roleId == null) return List.of();
        return jpaRepository.findByRoleId(UUID.fromString(roleId)).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoleMenu> findByRoleCode(String roleCode) {
        return jpaRepository.findByRoleCode(roleCode).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoleMenu> findByMenuId(String menuId) {
        if (menuId == null) return List.of();
        return jpaRepository.findByMenuId(UUID.fromString(menuId)).stream()
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
    public void deleteByRoleIdAndMenuId(String roleId, String menuId) {
        if (roleId != null && menuId != null) {
            jpaRepository.deleteByRoleIdAndMenuId(UUID.fromString(roleId), UUID.fromString(menuId));
        }
    }

    @Override
    public boolean existsByRoleIdAndMenuId(String roleId, String menuId) {
        if (roleId == null || menuId == null) return false;
        return jpaRepository.existsByRoleIdAndMenuId(UUID.fromString(roleId), UUID.fromString(menuId));
    }
}