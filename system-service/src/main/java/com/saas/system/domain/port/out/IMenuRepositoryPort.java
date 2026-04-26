package com.saas.system.domain.port.out;

import com.saas.common.port.out.ICodeRepositoryPort;
import com.saas.system.domain.model.Menu;

import java.util.List;
import java.util.UUID;

public interface IMenuRepositoryPort extends ICodeRepositoryPort<Menu, UUID> {

    List<Menu> findRootMenus();

    List<Menu> findByParentId(UUID parentId);

    /** Menus visibles para un set de roles (interseccion via menu_role). */
    List<Menu> findByRoleIds(java.util.Set<UUID> roleIds);
}
