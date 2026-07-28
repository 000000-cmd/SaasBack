package com.saas.system.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TourStepResponse(
        UUID id, UUID menuId, String menuCode, String anchor,
        String title, String body, String icon, Integer displayOrder,
        Boolean enabled, Boolean visible,
        LocalDateTime createdDate, LocalDateTime auditDate
) {}
