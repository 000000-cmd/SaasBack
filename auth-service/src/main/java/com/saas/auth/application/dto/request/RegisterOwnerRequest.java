package com.saas.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta self-service de un dueño + su negocio (registro mínimo, público).
 *
 * <p>auth-service crea la cuenta del dueño con el rol fijo {@code OWNER} y
 * devuelve los tokens de sesión. El negocio (nombre + slug) se captura
 * aquí; su aprovisionamiento completo en business-service es el paso posterior
 * (no se orquesta a ciegas entre microservicios desde este endpoint).
 */
public record RegisterOwnerRequest(
        @NotBlank @Size(max = 120) String businessName,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,63}$",
                message = "El slug admite minúsculas, números y guiones (3-63)") String slug,
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 60) String username,
        @NotBlank @Size(min = 8, max = 60) String password
) {}
