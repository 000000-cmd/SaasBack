package com.saas.system.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para asignación RoleMenu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleMenuResponse {

    private String id;
    private String roleId;
    private String roleCode;
    private String menuId;
    private String menuCode;
    private String menuLabel;
    private Boolean enabled;
    private LocalDateTime auditDate;

    // Permisos asignados a este RoleMenu
    private List<RoleMenuPermissionResponse> permissions;
}
