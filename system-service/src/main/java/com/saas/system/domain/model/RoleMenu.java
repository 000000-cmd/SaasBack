package com.saas.system.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modelo de dominio para la asignación de Menús a Roles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoleMenu extends BaseDomain {

    private String id;
    private String roleId;
    private String menuId;

    // Campos desnormalizados para conveniencia
    private String roleCode;
    private String menuCode;
    private String menuLabel;
}