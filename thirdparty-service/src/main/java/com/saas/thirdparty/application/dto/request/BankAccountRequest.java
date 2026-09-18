package com.saas.thirdparty.application.dto.request;

import com.saas.thirdparty.domain.model.AccountKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Alta o edicion de una cuenta.
 *
 * <p>Los campos de la forma van sueltos y sin {@code @NotNull} porque cuales son
 * obligatorios depende de {@code accountKind}. Esa regla vive en el servicio,
 * que es el unico sitio donde vale igual para la web, para el movil y para
 * cualquier alta futura.</p>
 */
public record BankAccountRequest(
        @NotNull UUID thirdPartyId,
        @NotNull AccountKind accountKind,
        UUID bankId,
        @Size(max = 16) String accountType,
        @Size(max = 40) String accountNumber,
        @Size(max = 120) String brevKey,
        @Size(max = 60) String alias,
        Boolean isPrimary
) {}
