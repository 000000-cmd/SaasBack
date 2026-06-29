package com.saas.thirdparty.application.dto.response;

import java.util.UUID;

public record ThirdPartyAddressResponse(
        UUID id,
        UUID thirdPartyId,
        UUID addressTypeId,
        UUID municipalityId,
        UUID neighborhoodId,
        String line,
        String reference,
        Boolean isPrimary,
        Boolean enabled
) {}
