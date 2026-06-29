package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public record BranchScheduleRequest(
        @NotNull UUID branchId,
        UUID businessScheduleId,
        @NotNull UUID scheduleTypeId,
        @NotBlank @Size(max = 120) String name,
        LocalDateTime validFrom
) {}
