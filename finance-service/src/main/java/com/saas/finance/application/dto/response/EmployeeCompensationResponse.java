package com.saas.finance.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeeCompensationResponse(
        UUID id, UUID employeeId, String compensationType, BigDecimal compensationValue, BigDecimal salaryBase,
        LocalDateTime validFrom, LocalDateTime validTo, Boolean enabled
) {}
