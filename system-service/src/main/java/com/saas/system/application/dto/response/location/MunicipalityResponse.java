package com.saas.system.application.dto.response.location;

import java.time.LocalDateTime;
import java.util.UUID;

public record MunicipalityResponse(
        UUID id,
        String code,
        String name,
        UUID departmentId,
        String departmentCode,
        String departmentName,
        UUID countryId,
        String countryCode,
        String countryName,
        Boolean enabled,
        Boolean visible,
        LocalDateTime createdDate,
        LocalDateTime auditDate
) {}
