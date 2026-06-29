package com.saas.business.application.dto.response;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record EmployeeShiftAssignmentResponse(
        UUID id, UUID employeeId, UUID branchScheduleShiftId, Boolean isFullShift,
        LocalTime customStartTime, LocalTime customEndTime, UUID statusId,
        LocalDateTime validFrom, LocalDateTime validTo, Boolean enabled
) {}
