package com.saas.business.application.dto.response;

import java.util.UUID;

public record ProvisionResponse(
        UUID businessId,
        String slug,
        UUID thirdPartyId,
        UUID businessOwnerId
) {}
