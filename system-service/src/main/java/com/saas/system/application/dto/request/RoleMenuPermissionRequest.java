package com.saas.system.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para asignar un Permiso a un RoleMenu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuPermissionRequest {

    @NotBlank(message = "El ID del RoleMenu es obligatorio")
    private String roleMenuId;

    @NotBlank(message = "El código del permiso es obligatorio")
    private String permissionCode;
}
