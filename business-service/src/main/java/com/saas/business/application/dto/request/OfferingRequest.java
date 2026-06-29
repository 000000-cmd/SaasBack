package com.saas.business.application.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record OfferingRequest(
        @NotNull UUID businessId,
        UUID categoryId,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description,
        @NotNull @Positive Integer durationMinutes,
        @NotNull @PositiveOrZero BigDecimal price,
        Boolean isActive
) {}
