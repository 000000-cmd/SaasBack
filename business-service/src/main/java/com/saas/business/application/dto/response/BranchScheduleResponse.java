package com.saas.business.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BranchScheduleResponse(
        UUID id, UUID branchId, UUID businessScheduleId, UUID scheduleTypeId, String name,
        LocalDateTime validFrom, LocalDateTime validTo, Boolean enabled
) {}
