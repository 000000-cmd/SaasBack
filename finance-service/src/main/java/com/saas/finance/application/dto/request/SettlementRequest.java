package com.saas.finance.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Confirmacion de liquidacion. {@code amount} es opcional: sin el se liquida
 * todo el saldo por cobrar del empleado.
 */
public record SettlementRequest(
        @NotNull UUID employeeId,
        @DecimalMin(value = "0.01", message = "El monto a liquidar debe ser mayor que cero") BigDecimal amount,
        @Size(max = 255) String note
) {}
