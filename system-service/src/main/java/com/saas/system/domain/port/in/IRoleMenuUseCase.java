package com.saas.system.domain.port.in;

import com.saas.system.domain.model.RoleMenu;

import java.util.List;

/**
 * Puerto de entrada (caso de uso) para asignaciones de Menús a Roles.
 */
public interface IRoleMenuUseCase {

    /**
     * Asigna un menú a un rol
     */
    RoleMenu assignMenuToRole(String roleCode, String menuCode);

    /**
     * Obtiene los menús asignados a un rol
     */
    List<RoleMenu> getMenusByRoleCode(String roleCode);

    /**
     * Elimina una asignación por su ID
     */
    void removeAssignment(String id);

    /**
     * Elimina una asignación específica por rol y menú
     */
    void removeAssignment(String roleCode, String menuCode);
}