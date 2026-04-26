package com.saas.system.domain.port.in;

import com.saas.system.domain.model.Permission;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IRolePermissionUseCase {

    List<Permission> getPermissionsByRoleId(UUID roleId);

    Set<String> getPermissionCodesByRoleId(UUID roleId);

    void replacePermissionsForRole(UUID roleId, Set<UUID> permissionIds);
}
