package com.saas.business.application.dto.response;

import java.util.UUID;

public record BookingPolicyResponse(
        UUID id, UUID businessId,
        Boolean requiresManualConfirmation, Integer confirmationTimeoutMinutes,
        Integer minLeadTimeMinutes, Integer maxHorizonDays,
        Integer slotGranularityMinutes, Integer bufferBeforeMinutes, Integer bufferAfterMinutes,
        Integer clientCancelWindowHours, Boolean requirePhoneVerification,
        String reminderHoursBefore,
        Boolean whatsappEnabled, String whatsappPhoneId, Integer whatsappMonthlyCount
) {}
