package com.saas.business.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BranchOfferingResponse(
        UUID id, UUID branchId, UUID offeringId, String name, String description,
        Integer durationMinutes, BigDecimal price, Boolean isEnabled, Boolean isActive, Boolean enabled
) {}
