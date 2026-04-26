package com.saas.system.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermissionRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description
) {}
