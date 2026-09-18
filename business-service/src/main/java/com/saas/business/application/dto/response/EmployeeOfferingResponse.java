package com.saas.business.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record EmployeeOfferingResponse(
        UUID id, UUID employeeId, UUID offeringId,
        Integer durationMinutes, BigDecimal commissionRate, Boolean enabled
) {}
