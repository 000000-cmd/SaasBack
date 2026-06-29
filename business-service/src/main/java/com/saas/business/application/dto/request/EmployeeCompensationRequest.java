package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record EmployeeCompensationRequest(
        @NotNull UUID employeeId,
        @NotBlank String compensationType,
        BigDecimal baseSalary,
        BigDecimal servicePercentage,
        BigDecimal fixedCommission
) {}
