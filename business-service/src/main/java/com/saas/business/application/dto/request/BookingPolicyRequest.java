package com.saas.business.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * La configuracion de reservas que el dueno puede tocar.
 *
 * <p>Los contadores de WhatsApp no estan: son del sistema, no configuracion.</p>
 */
public record BookingPolicyRequest(
        Boolean requiresManualConfirmation,
        @Min(5) @Max(1440) Integer confirmationTimeoutMinutes,
        @Min(0) @Max(10080) Integer minLeadTimeMinutes,
        @Min(1) @Max(365) Integer maxHorizonDays,
        @Min(5) @Max(120) Integer slotGranularityMinutes,
        @Min(0) @Max(240) Integer bufferBeforeMinutes,
        @Min(0) @Max(240) Integer bufferAfterMinutes,
        @Min(0) @Max(720) Integer clientCancelWindowHours,
        Boolean requirePhoneVerification,
        /** Horas antes a las que se recuerda, separadas por coma ("24,2"). */
        @jakarta.validation.constraints.Pattern(
                regexp = "^$|^ *[0-9]{1,3} *(, *[0-9]{1,3} *)*$",
                message = "Escribe las horas separadas por coma, por ejemplo 24,2")
        @jakarta.validation.constraints.Size(max = 40)
        String reminderHoursBefore,
        Boolean whatsappEnabled,
        /** Id del número de WhatsApp Business (lo da Meta). */
        @jakarta.validation.constraints.Size(max = 40) String whatsappPhoneId
) {}
