package com.saas.system.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SystemListItemResponse(
        UUID id, UUID listId, String code, String name, String value,
        Integer displayOrder,
        Boolean enabled, Boolean visible,
        LocalDateTime createdDate, LocalDateTime auditDate
) {}
