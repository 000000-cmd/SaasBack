package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SpecialtyRequest(
        @NotNull UUID businessId,
        @NotBlank @Size(max = 120) String name,
        Integer displayOrder
) {}
