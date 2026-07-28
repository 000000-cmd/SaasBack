package com.saas.system.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TourStepRequest(
        UUID menuId,
        @Size(max = 60) String anchor,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 500) String body,
        @Size(max = 60) String icon,
        @NotNull Integer displayOrder
) {}
