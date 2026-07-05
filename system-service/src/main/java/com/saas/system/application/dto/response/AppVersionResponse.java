package com.saas.system.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/** Vista de administración de una versión del APK (histórico). */
public record AppVersionResponse(
        UUID id,
        String version,
        Integer versionCode,
        String fileName,
        String checksum,
        Long sizeBytes,
        String notes,
        Boolean isCurrent,
        Boolean enabled,
        Boolean visible,
        LocalDateTime createdDate,
        LocalDateTime auditDate
) {}
