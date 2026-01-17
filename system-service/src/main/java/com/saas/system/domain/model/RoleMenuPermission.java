package com.saas.system.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modelo de dominio para los Permisos asignados a un RoleMenu.
 * Define qué puede hacer un rol en un menú específico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoleMenuPermission extends BaseDomain {

    private String id;
    private String roleMenuId;
    private String permissionId;

    // Campos desnormalizados para conveniencia
    private String permissionCode;
    private String permissionName;
}