package com.saas.business.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BusinessDomainResponse(
        UUID id, UUID businessId, String slug, String customDomain,
        Boolean isPrimary, Boolean isVerified, LocalDateTime verifiedDate,
        UUID statusId,
        Boolean enabled, Boolean visible, LocalDateTime createdDate, LocalDateTime auditDate
) {}
