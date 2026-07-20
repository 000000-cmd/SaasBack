package com.saas.finance.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeeBalanceResponse(
        UUID id, UUID businessId, UUID branchId, UUID employeeId, UUID thirdPartyId, UUID userId,
        BigDecimal amountAccrued, BigDecimal amountPaid, BigDecimal balance,
        String currency, LocalDateTime lastCalculatedAt
) {}
