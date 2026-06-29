package com.saas.business.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BusinessResponse(
        UUID id, UUID businessTypeId, String name, String legalName, String tradeName,
        UUID documentTypeId, String documentNumber, String logoUrl, UUID statusId,
        String primaryColor, String secondaryColor,
        Boolean enabled, Boolean visible, LocalDateTime createdDate, LocalDateTime auditDate
) {}
