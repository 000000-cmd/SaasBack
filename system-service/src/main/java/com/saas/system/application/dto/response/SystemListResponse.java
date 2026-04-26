package com.saas.system.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SystemListResponse(
        UUID id, String code, String name, String description,
        Boolean enabled, Boolean visible,
        LocalDateTime createdDate, LocalDateTime auditDate
) {}
