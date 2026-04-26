package com.saas.system.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConstantRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 1000) String value,
        @Size(max = 500) String description
) {}
