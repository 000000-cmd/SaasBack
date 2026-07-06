package com.saas.business.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Compensacion efectiva resuelta jerarquicamente.
 * {@code source} = EMPLOYEE | BRANCH | BUSINESS (nivel que la aporto);
 * {@code sourceId} = id de esa entidad.
 */
public record EffectiveCompensationResponse(
        String source, UUID sourceId, String compensationType, BigDecimal compensationValue,
        LocalDateTime validFrom, LocalDateTime validTo
) {}
