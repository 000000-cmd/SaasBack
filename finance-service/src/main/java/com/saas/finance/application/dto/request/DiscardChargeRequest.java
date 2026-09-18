package com.saas.finance.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Rechazo o reverso de un servicio. El motivo queda en el historial.
 *
 * <p>Obligatorio: al otro lado hay un empleado al que le quitaron un servicio,
 * y "descartado por el dueño" no le explica nada.</p>
 */
public record DiscardChargeRequest(
        @NotBlank(message = "Escribe el motivo: el empleado va a verlo")
        @Size(max = 300) String reason
) {}
