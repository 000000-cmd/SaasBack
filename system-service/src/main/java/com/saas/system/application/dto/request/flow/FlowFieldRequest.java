package com.saas.system.application.dto.request.flow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param code la clave con la que el dato viaja al servidor ("phone").
 * @param maskCode mascara del estandar compartido con el APK. Null = texto libre.
 * @param sourceKey de donde salen las opciones de un select.
 */
public record FlowFieldRequest(
        @NotBlank @Size(max = 60) String code,
        @NotBlank @Size(max = 120) String label,
        @Size(max = 120) String placeholder,
        @Size(max = 300) String helpText,
        @Size(max = 20) String dataType,
        @Size(max = 20) String maskCode,
        @Size(max = 60) String sourceKey,
        Boolean isRequired,
        @Size(max = 10) String width,
        Integer displayOrder,
        Boolean enabled
) {}
