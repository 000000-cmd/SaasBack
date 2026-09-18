package com.saas.finance.application.dto.response;

import com.saas.finance.domain.model.ChargeStatus;
import com.saas.finance.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Un servicio tal como lo pinta la factura de la pantalla.
 *
 * <p>{@code missingReceipt} viaja calculado desde el dominio en vez de dejar que
 * el front vuelva a deducirlo: la regla ("los pagos electronicos exigen
 * comprobante") es una sola y se decide en un solo sitio.</p>
 */
public record ServiceChargeResponse(
        UUID id,
        UUID businessId,
        UUID branchId,
        UUID employeeId,
        UUID appointmentId,
        String serviceName,
        LocalDate serviceDate,
        LocalTime startTime,
        LocalTime endTime,
        String clientName,
        UUID clientThirdPartyId,
        String clientEmail,
        String clientPhone,
        BigDecimal grossAmount,
        BigDecimal deductionRate,
        BigDecimal deductionAmount,
        BigDecimal netAmount,
        String currency,
        PaymentMethod paymentMethod,
        String receiptUrl,
        String resultPhotoUrl,
        ChargeStatus status,
        LocalDateTime confirmedAt,
        LocalDateTime discardedAt,
        String discardReason,
        UUID settlementId,
        boolean missingReceipt
) {}
