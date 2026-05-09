package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.MenuRole;
import com.saas.system.domain.port.out.IMenuRoleRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.MenuEntity;
import com.saas.system.infrastructure.persistence.entity.MenuRoleEntity;
import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import com.saas.system.infrastructure.persistence.mapper.MenuRolePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaMenuRoleRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class MenuRoleRepositoryAdapter
        extends BaseJpaRepositoryAdapter<MenuRole, MenuRoleEntity, UUID>
        implements IMenuRoleRepositoryPort {

    private final JpaMenuRoleRepository jpa;

    public MenuRoleRepositoryAdapter(JpaMenuRoleRepository jpa,
                                     MenuRolePersistenceMapper mapper) {
        super(jpa, mapper, "MenuRole");
        this.jpa = jpa;
    }

    @Override
    public List<MenuRole> findByMenuId(UUID menuId) {
        return getMapper().toDomainList(jpa.findByMenuId(menuId));
    }

    @Override
    public List<MenuRole> findByRoleId(UUID roleId) {
        return getMapper().toDomainList(jpa.findByRoleId(roleId));
    }

    @Override
    public boolean existsByMenuIdAndRoleId(UUID menuId, UUID roleId) {
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
}
