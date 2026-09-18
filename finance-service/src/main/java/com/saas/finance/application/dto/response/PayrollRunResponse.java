package com.saas.finance.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Una corrida de nomina tal como la lista el historial. */
public record PayrollRunResponse(
        UUID id, UUID businessId, UUID branchId,
        String code, String periodLabel, LocalDate periodStart, LocalDate periodEnd,
        Integer employeeCount, BigDecimal totalAmount, String currency,
        String status, LocalDateTime executedAt, String note,
        /** Cuando se anulo, y por que. Nulos = la corrida sigue en pie. */
        LocalDateTime voidedAt, String voidReason
) {}
