package com.saas.business.application.dto.response;

import java.time.LocalTime;
import java.util.UUID;

public record BranchScheduleShiftResponse(
        UUID id, UUID branchScheduleId, UUID shiftTypeId, UUID dayOfWeekId,
        LocalTime startTime, LocalTime endTime, Integer displayOrder, Boolean enabled
) {}
