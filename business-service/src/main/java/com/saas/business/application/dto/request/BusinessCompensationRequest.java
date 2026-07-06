package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record BusinessCompensationRequest(
        @NotNull UUID businessId,
        @NotBlank String compensationType,
        @NotNull BigDecimal compensationValue
) {}
