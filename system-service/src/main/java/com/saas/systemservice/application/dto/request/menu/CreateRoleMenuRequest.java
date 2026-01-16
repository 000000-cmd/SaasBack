package com.saas.systemservice.application.dto.request.menu;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateRoleMenuRequest {

    @NotBlank(message = "El código del rol es obligatorio")
    private String roleCode;

    @NotBlank(message = "El código del menú es obligatorio")
    private String menuCode;
}