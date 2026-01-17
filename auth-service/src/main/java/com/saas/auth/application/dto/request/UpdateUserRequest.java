package com.saas.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar un usuario existente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Email(message = "El email debe tener un formato válido")
    private String email;

    @Size(max = 20, message = "El celular no puede exceder 20 caracteres")
    private String cellular;

    private String attachment;
}