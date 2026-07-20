package com.saas.finance.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record BranchCompensationRequest(
        @NotNull UUID branchId,
        @NotBlank String compensationType,
        @NotNull BigDecimal compensationValue,
        BigDecimal salaryBase
) {}
