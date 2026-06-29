package com.saas.business.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BranchResponse(
        UUID id, UUID businessId, UUID branchTypeId, String name, String code,
        UUID municipalityId, UUID neighborhoodId, String addressLine, String phone,
        Boolean isMain, UUID statusId,
        Boolean enabled, Boolean visible, LocalDateTime createdDate, LocalDateTime auditDate
) {}
