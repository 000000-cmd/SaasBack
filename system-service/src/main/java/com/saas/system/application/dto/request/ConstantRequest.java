package com.saas.system.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar una Constante.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConstantRequest {

    @NotBlank(message = "El código es obligatorio")
    @Size(min = 2, max = 50, message = "El código debe tener entre 2 y 50 caracteres")
    private String code;

    @NotBlank(message = "El valor es obligatorio")
    @Size(max = 500, message = "El valor no puede exceder 500 caracteres")
    private String value;

    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String description;

    @Size(max = 100, message = "La categoría no puede exceder 100 caracteres")
    private String category;
}