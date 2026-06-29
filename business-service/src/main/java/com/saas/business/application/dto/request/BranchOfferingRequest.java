package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record BranchOfferingRequest(
        @NotNull UUID branchId,
        UUID offeringId,
        @Size(max = 160) String name,
        @Size(max = 500) String description,
        Integer durationMinutes,
        BigDecimal price,
        Boolean isEnabled,
        Boolean isActive
) {}
