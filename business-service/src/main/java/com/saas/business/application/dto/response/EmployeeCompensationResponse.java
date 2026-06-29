package com.saas.business.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeeCompensationResponse(
        UUID id, UUID employeeId, String compensationType, BigDecimal baseSalary,
        BigDecimal servicePercentage, BigDecimal fixedCommission,
        LocalDateTime validFrom, LocalDateTime validTo, Boolean enabled
) {}
