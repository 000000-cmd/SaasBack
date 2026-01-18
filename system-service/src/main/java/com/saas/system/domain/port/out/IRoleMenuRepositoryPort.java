package com.saas.system.domain.port.out;

import com.saas.system.domain.model.RoleMenu;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de asignaciones RoleMenu.
 */
public interface IRoleMenuRepositoryPort {

    /**
     * Guarda una asignación RoleMenu
     */
    RoleMenu save(RoleMenu roleMenu);

    /**
     * Busca una asignación por ID
     */
    Optional<RoleMenu> findById(String id);

    /**
     * Obtiene todas las asignaciones de un rol por su ID
     */
    List<RoleMenu> findByRoleId(String roleId);

    /**
     * Obtiene todas las asignaciones de un rol por su código
     */
    List<RoleMenu> findByRoleCode(String roleCode);

    /**
     * Obtiene todas las asignaciones de un menú
     */
    List<RoleMenu> findByMenuId(String menuId);

    /**
     * Elimina una asignación por su ID
     */
    void deleteById(String id);

    /**
     * Elimina una asignación por rol y menú
     */
    void deleteByRoleIdAndMenuId(String roleId, String menuId);

    /**
     * Verifica si existe una asignación para un rol y menú
     */
    boolean existsByRoleIdAndMenuId(String roleId, String menuId);
}