package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.MenuRole;
import com.saas.system.domain.port.out.IMenuRoleRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.MenuEntity;
import com.saas.system.infrastructure.persistence.entity.MenuRoleEntity;
import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import com.saas.system.infrastructure.persistence.mapper.MenuRolePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaMenuRoleRepository;
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
public class MenuRoleRepositoryAdapter implements IMenuRoleRepositoryPort {

    private final JpaMenuRoleRepository jpa;
    private final MenuRolePersistenceMapper mapper;

    @Override
    public MenuRole save(MenuRole domain) {
        return mapper.toDomain(jpa.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional
    public MenuRole update(MenuRole domain) {
        MenuRoleEntity existing = jpa.findById(domain.getId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuRole", "Id", domain.getId()));
        mapper.updateEntityFromDomain(domain, existing);
        return mapper.toDomain(jpa.save(existing));
    }

    @Override public Optional<MenuRole> findById(UUID id)         { return jpa.findById(id).map(mapper::toDomain); }
    @Override public boolean            existsById(UUID id)       { return jpa.existsById(id); }
    @Override public List<MenuRole>     findAll()                  { return mapper.toDomainList(jpa.findAll()); }
    @Override public List<MenuRole>     findByMenuId(UUID menuId)  { return mapper.toDomainList(jpa.findByMenuId(menuId)); }
    @Override public List<MenuRole>     findByRoleId(UUID roleId)  { return mapper.toDomainList(jpa.findByRoleId(roleId)); }
    @Override public boolean existsByMenuIdAndRoleId(UUID menuId, UUID roleId) {
        return jpa.existsByMenuIdAndRoleId(menuId, roleId);
    }

    @Override
    @Transactional
    public void replaceRolesForMenu(UUID menuId, Set<UUID> roleIds) {
        Set<UUID> desired = roleIds == null ? Set.of() : new HashSet<>(roleIds);
        List<MenuRoleEntity> current = jpa.findByMenuId(menuId);
        Set<UUID> currentIds = current.stream()
                .map(mr -> mr.getRole().getId())
                .collect(Collectors.toSet());

        current.stream()
                .filter(mr -> !desired.contains(mr.getRole().getId()))
                .forEach(jpa::delete);

        MenuEntity menuRef = new MenuEntity();
        menuRef.setId(menuId);

        desired.stream()
                .filter(rid -> !currentIds.contains(rid))
                .map(rid -> {
                    RoleEntity r = new RoleEntity();
                    r.setId(rid);
                    return MenuRoleEntity.builder().menu(menuRef).role(r).build();
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
