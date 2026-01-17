package com.saas.system.domain.port.in;

import com.saas.system.domain.model.RoleMenuPermission;

import java.util.List;

/**
 * Puerto de entrada (caso de uso) para permisos de RoleMenu.
 */
public interface IRoleMenuPermissionUseCase {

    /**
     * Asigna un permiso a un RoleMenu
     */
    RoleMenuPermission assignPermission(String roleMenuId, String permissionCode);

    /**
     * Obtiene los permisos de un RoleMenu
     */
    List<RoleMenuPermission> getPermissionsByRoleMenuId(String roleMenuId);

    /**
     * Elimina un permiso de un RoleMenu
     */
    void removePermission(String id);

    /**
     * Elimina todos los permisos de un RoleMenu
     */
    void removeAllPermissionsFromRoleMenu(String roleMenuId);
}
