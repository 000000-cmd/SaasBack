package com.saas.finance.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeeSettlementResponse(
        UUID id, UUID businessId, UUID branchId, UUID employeeId,
        BigDecimal amount, BigDecimal balanceBefore, String currency,
        LocalDateTime settledAt, String note
) {}
