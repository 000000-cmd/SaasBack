package com.saas.business.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BusinessOwnerResponse(
        UUID id, UUID businessId, UUID thirdPartyId, BigDecimal ownershipPercentage,
        LocalDate startDate, LocalDate endDate,
        Boolean enabled, Boolean visible, LocalDateTime createdDate, LocalDateTime auditDate
) {}
