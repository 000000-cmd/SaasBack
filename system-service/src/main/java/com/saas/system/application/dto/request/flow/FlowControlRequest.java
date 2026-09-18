package com.saas.system.application.dto.request.flow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param action NEXT | BACK | SAVE | CONFIRM | CANCEL | EDIT | DELETE | CUSTOM.
 *        El comportamiento lo pone el front; aqui solo se dice cual es.
 * @param permissionCode permiso que exige. Null = cualquiera que llegue.
 */
public record FlowControlRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String label,
        @NotBlank @Size(max = 20) String action,
        @Size(max = 16) String variant,
        @Size(max = 50) String permissionCode,
        @Size(max = 120) String channels,
        Integer displayOrder,
        Boolean enabled
) {}
