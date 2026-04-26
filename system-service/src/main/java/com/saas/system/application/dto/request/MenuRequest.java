package com.saas.system.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MenuRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 60) String icon,
        @Size(max = 200) String route,
        UUID parentId,
        @NotNull Integer displayOrder
) {}
