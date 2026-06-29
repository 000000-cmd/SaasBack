package com.saas.business.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClientResponse(
        UUID id, UUID thirdPartyId, UUID registrationStatusId, String acquisitionSource, String notes,
        Boolean enabled, Boolean visible, LocalDateTime createdDate, LocalDateTime auditDate
) {}
