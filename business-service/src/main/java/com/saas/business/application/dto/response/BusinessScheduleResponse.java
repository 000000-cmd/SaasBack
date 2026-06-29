package com.saas.business.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BusinessScheduleResponse(
        UUID id, UUID businessId, UUID scheduleTypeId, String name,
        LocalDateTime validFrom, LocalDateTime validTo, Boolean enabled
) {}
