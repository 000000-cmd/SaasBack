package com.saas.business.application.dto.response;

import java.time.LocalTime;
import java.util.UUID;

public record BusinessScheduleShiftResponse(
        UUID id, UUID businessScheduleId, UUID shiftTypeId, UUID dayOfWeekId,
        LocalTime startTime, LocalTime endTime, Integer displayOrder, Boolean enabled
) {}
