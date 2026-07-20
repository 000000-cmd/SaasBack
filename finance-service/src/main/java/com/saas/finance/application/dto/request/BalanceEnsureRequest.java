package com.saas.finance.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Alta/aseguramiento del saldo de un empleado (S2S, la dispara business-service
 * al aprovisionar el empleado). Crea la fila en 0 si no existe y la proyecta a ES.
 */
public record BalanceEnsureRequest(
        @NotNull UUID employeeId,
        @NotNull UUID businessId,
        UUID branchId,
        UUID thirdPartyId,
        UUID userId
) {}
