package com.saas.thirdparty.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ThirdPartyContactResponse(
        UUID id,
        UUID thirdPartyId,
        UUID contactTypeId,
        String value,
        Boolean isPrimary,
        Boolean isVerified,
        LocalDateTime verifiedAt,
        String notes,
        Boolean enabled
) {}
