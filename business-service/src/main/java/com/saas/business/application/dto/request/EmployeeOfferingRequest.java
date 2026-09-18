package com.saas.business.application.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un servicio que presta un empleado.
 *
 * <p>Duracion y comision son nulables: {@code null} = hereda del servicio.</p>
 */
public record EmployeeOfferingRequest(
        @NotNull UUID offeringId,
        @Min(1) Integer durationMinutes,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal commissionRate
) {}
