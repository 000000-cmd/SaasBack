package com.saas.systemservice.application.dto.request.menu;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateMenuRequest {

    @NotBlank(message = "El código es obligatorio")
    @Size(min = 3, max = 50, message = "El código debe tener entre 3 y 50 caracteres")
    private String code;

    @NotBlank(message = "La etiqueta (label) es obligatoria")
    @Size(max = 50)
    private String label;

    @Size(max = 200)
    private String routerLink;

    @Size(max = 50)
    private String icon;

    @Min(value = 0, message = "El orden debe ser un número positivo")
    private Integer order;

    private String parentId; // Opcional (puede ser null si es raíz)
}
