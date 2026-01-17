package com.saas.system.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar un Menú.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuRequest {

    @NotBlank(message = "El código es obligatorio")
    @Size(min = 2, max = 50, message = "El código debe tener entre 2 y 50 caracteres")
    private String code;

    @NotBlank(message = "La etiqueta es obligatoria")
    @Size(max = 100, message = "La etiqueta no puede exceder 100 caracteres")
    private String label;

    @Size(max = 255, message = "El router link no puede exceder 255 caracteres")
    private String routerLink;

    @Size(max = 50, message = "El icono no puede exceder 50 caracteres")
    private String icon;

    private Integer displayOrder;

    private String parentId;
}
