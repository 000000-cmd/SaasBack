package com.saas.system.application.dto.request.flow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param code el identificador con el que el software lo invoca. Mayusculas y
 *        sin espacios: se escribe en codigo, no se traduce.
 */
public record FlowRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Z0-9_]+$",
                message = "El código solo admite mayúsculas, números y guion bajo")
        String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        Boolean enabled
) {}
