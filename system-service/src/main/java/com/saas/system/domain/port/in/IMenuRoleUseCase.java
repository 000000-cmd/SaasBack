package com.saas.system.domain.port.in;

import java.util.Set;
import java.util.UUID;

public interface IMenuRoleUseCase {

    void replaceRolesForMenu(UUID menuId, Set<UUID> roleIds);

    Set<UUID> getRoleIdsForMenu(UUID menuId);
}
