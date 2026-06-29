package com.saas.thirdparty.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ThirdPartyAddressRequest(
        @NotNull UUID thirdPartyId,
        UUID addressTypeId,
        @NotNull UUID municipalityId,
        UUID neighborhoodId,
        @Size(max = 255) String line,
        @Size(max = 255) String reference,
        Boolean isPrimary
) {}
