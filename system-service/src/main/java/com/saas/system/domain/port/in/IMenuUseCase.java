package com.saas.system.domain.port.in;

import com.saas.common.port.in.ICodeUseCase;
import com.saas.system.domain.model.Menu;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IMenuUseCase extends ICodeUseCase<Menu, UUID> {

    List<Menu> getRootMenus();

    List<Menu> getChildren(UUID parentId);

    /** Menus visibles para un usuario dado su set de roles. */
    List<Menu> getMenusForRoles(Set<UUID> roleIds);
}
