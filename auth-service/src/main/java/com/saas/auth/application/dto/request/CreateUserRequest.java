package com.saas.auth.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(max = 60) String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 8, max = 60) String password,
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        String profilePhoto,
        String theme,
        String languageCode,
        Set<UUID> roleIds
) {}
