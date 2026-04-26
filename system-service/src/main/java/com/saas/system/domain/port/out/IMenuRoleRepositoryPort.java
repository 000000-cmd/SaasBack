package com.saas.system.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.MenuRole;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IMenuRoleRepositoryPort extends IGenericRepositoryPort<MenuRole, UUID> {

    List<MenuRole> findByMenuId(UUID menuId);

    List<MenuRole> findByRoleId(UUID roleId);

    void replaceRolesForMenu(UUID menuId, Set<UUID> roleIds);

    boolean existsByMenuIdAndRoleId(UUID menuId, UUID roleId);
}
