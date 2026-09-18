package com.saas.finance.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Confirmacion de liquidacion.
 *
 * <p>Ya NO lleva monto: se liquida la suma de los servicios aprobados y sin
 * liquidar del empleado. Dejar que el cliente mandara una cifra era permitir
 * abonar un numero que no corresponde a ningun trabajo.</p>
 */
public record SettlementRequest(
        @NotNull UUID employeeId,
        @Size(max = 255) String note
) {}
