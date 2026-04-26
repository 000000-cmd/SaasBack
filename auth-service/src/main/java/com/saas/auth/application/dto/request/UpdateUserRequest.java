package com.saas.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Endpoint admin: actualiza datos de un usuario.
 * Campos null = no se modifican (se aprovecha el patron IGNORE de updateEntityFromDomain).
 */
public record UpdateUserRequest(
        @Size(max = 60) String username,
        @Email @Size(max = 120) String email,
        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        String profilePhoto,
        String theme,
        String languageCode
) {}
