package com.saas.system.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para asignar un Menú a un Rol.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuRequest {

    @NotBlank(message = "El código del rol es obligatorio")
    private String roleCode;

    @NotBlank(message = "El código del menú es obligatorio")
    private String menuCode;
}