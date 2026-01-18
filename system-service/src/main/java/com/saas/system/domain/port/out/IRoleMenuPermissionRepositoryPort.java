package com.saas.system.domain.port.out;

import com.saas.system.domain.model.RoleMenuPermission;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de permisos de RoleMenu.
 */
public interface IRoleMenuPermissionRepositoryPort {

    /**
     * Guarda una asignación de permiso a RoleMenu
     */
    RoleMenuPermission save(RoleMenuPermission permission);

    /**
     * Busca una asignación por ID
     */
    Optional<RoleMenuPermission> findById(String id);

    /**
     * Obtiene todos los permisos de un RoleMenu
     */
    List<RoleMenuPermission> findByRoleMenuId(String roleMenuId);

    /**
     * Elimina una asignación por su ID
     */
    void deleteById(String id);

    /**
     * Elimina todas las asignaciones de un RoleMenu
     */
    void deleteByRoleMenuId(String roleMenuId);

    /**
     * Elimina una asignación específica por RoleMenu y permiso
     */
    void deleteByRoleMenuIdAndPermissionId(String roleMenuId, String permissionId);

    /**
     * Verifica si existe una asignación para un RoleMenu y permiso
     */
    boolean existsByRoleMenuIdAndPermissionId(String roleMenuId, String permissionId);
}