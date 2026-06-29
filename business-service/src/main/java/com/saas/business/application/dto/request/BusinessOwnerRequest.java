package com.saas.business.application.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BusinessOwnerRequest(
        @NotNull UUID businessId,
        @NotNull UUID thirdPartyId,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal ownershipPercentage,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {}
