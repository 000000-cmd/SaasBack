package com.saas.system.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar definiciones de listas del sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListDefinitionRequest {

    @NotBlank(message = "El nombre para mostrar es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String displayName;

    @NotBlank(message = "El nombre de la tabla física es obligatorio")
    @Size(max = 100, message = "El nombre de tabla no puede exceder 100 caracteres")
    @Pattern(regexp = "^sys_list_[a-z_]+$",
            message = "El nombre de tabla debe seguir el patrón: sys_list_nombre_en_minusculas")
    private String physicalTableName;
}
