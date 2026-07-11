package com.saas.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Verificación de disponibilidad de correo (paso 1 del registro). */
public record EmailCheckRequest(
        @NotBlank String email
) {}
