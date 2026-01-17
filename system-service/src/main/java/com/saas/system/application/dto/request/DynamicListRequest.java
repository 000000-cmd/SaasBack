package com.saas.system.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO genérico para crear/actualizar items de cualquier lista dinámica.
 * Todas las listas comparten la misma estructura de campos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicListRequest {

    @NotBlank(message = "El código es obligatorio")
    @Size(min = 1, max = 50, message = "El código debe tener entre 1 y 50 caracteres")
    private String code;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String name;

    @NotNull(message = "El orden es obligatorio")
    private Integer displayOrder;
}