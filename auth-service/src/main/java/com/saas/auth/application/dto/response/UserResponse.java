package com.saas.auth.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String profilePhoto,
        String theme,
        String languageCode,
        LocalDateTime lastLoginAt,
        Boolean enabled,
        Boolean visible,
        Set<String> roleCodes,
        LocalDateTime createdDate
) {}
