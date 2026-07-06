package com.saas.business.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BranchCompensationResponse(
        UUID id, UUID branchId, String compensationType, BigDecimal compensationValue,
        LocalDateTime validFrom, LocalDateTime validTo, Boolean enabled
) {}
