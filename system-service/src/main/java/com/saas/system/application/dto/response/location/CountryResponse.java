package com.saas.system.application.dto.response.location;

import java.time.LocalDateTime;
import java.util.UUID;

public record CountryResponse(
        UUID id,
        String code,
        String name,
        String officialName,
        String isoCode3,
        String numericCode,
        String phoneCode,
        String currencyCode,
        String currencySymbol,
        String continent,
        Boolean enabled,
        Boolean visible,
        LocalDateTime createdDate,
        LocalDateTime auditDate
) {}
