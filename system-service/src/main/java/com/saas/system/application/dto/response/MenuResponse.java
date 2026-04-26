package com.saas.system.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MenuResponse(
        UUID id, String code, String name, String icon, String route,
        UUID parentId, Integer displayOrder,
        Boolean enabled, Boolean visible,
        LocalDateTime createdDate, LocalDateTime auditDate,
        List<MenuResponse> children
) {}
