package com.saas.system.application.dto.request.flow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param kind FORM | SELECTION | REVIEW | INFO. Solo FORM lee campos.
 * @param channels CSV de canales: WEB,PANEL,APK,WHATSAPP. Vacio = todos.
 */
public record FlowSectionRequest(
        @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Z0-9_]+$",
                message = "El código solo admite mayúsculas, números y guion bajo")
        String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        Integer displayOrder,
        String kind,
        @Size(max = 120) String channels,
        Boolean enabled
) {}
