package com.saas.system.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.RolePermission;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IRolePermissionRepositoryPort extends IGenericRepositoryPort<RolePermission, UUID> {

    List<RolePermission> findByRoleId(UUID roleId);

    /** Codigos de permisos asignados a un rol (resolucion para JWT). */
    Set<String> findPermissionCodesByRoleId(UUID roleId);

    void replacePermissionsForRole(UUID roleId, Set<UUID> permissionIds);

    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
}
