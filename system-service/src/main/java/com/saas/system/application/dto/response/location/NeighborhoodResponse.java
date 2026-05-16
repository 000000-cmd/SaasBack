package com.saas.system.application.dto.response.location;

import java.time.LocalDateTime;
import java.util.UUID;

public record NeighborhoodResponse(
        UUID id,
        String code,
        String name,
        String type,
        UUID municipalityId,
        String municipalityCode,
        String municipalityName,
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
