package com.saas.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta self-service de un dueño (registro MÍNIMO, público).
 *
 * <p>Solo la cuenta: auth-service crea el usuario con el rol fijo {@code OWNER} y
 * devuelve los tokens de sesión. Los datos del negocio (tipo, nombre, slug) NO se
 * piden aquí: se completan después en el flujo de "completar empresa" (modal +
 * widget del dashboard), que pre-rellena el nombre desde esta cuenta — así no se
 * duplica lo ya capturado.</p>
 */
public record RegisterOwnerRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 60) String username,
        @NotBlank @Size(min = 8, max = 60) String password
) {}
