package com.saas.thirdparty.application.dto.response;

import com.saas.thirdparty.domain.model.AccountKind;

import java.util.UUID;

/**
 * Una cuenta lista para pintar.
 *
 * <p>{@code bankName} y {@code label} viajan resueltos: quien la muestra (la
 * ficha del empleado, el asistente de nomina, el comprobante) no deberia tener
 * que pedir el catalogo de bancos ni recomponer el texto por su cuenta.</p>
 */
public record BankAccountResponse(
        UUID id,
        UUID thirdPartyId,
        AccountKind accountKind,
        UUID bankId,
        String bankName,
        String accountType,
        String accountNumber,
        String brevKey,
        String alias,
        Boolean isPrimary,
        /** "Bancolombia · Ahorros · ···4590" o "Llave BREV · 3001234567". */
        String label
) {}
