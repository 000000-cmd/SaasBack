package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.UUID;

public record BusinessScheduleShiftRequest(
        @NotNull UUID businessScheduleId,
        @NotNull UUID shiftTypeId,
        @NotNull UUID dayOfWeekId,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Integer displayOrder
) {}
