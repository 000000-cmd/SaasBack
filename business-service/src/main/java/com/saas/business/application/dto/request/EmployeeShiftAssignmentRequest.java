package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.UUID;

public record EmployeeShiftAssignmentRequest(
        @NotNull UUID employeeId,
        @NotNull UUID branchScheduleShiftId,
        @NotNull Boolean isFullShift,
        LocalTime customStartTime,
        LocalTime customEndTime,
        UUID statusId
) {}
