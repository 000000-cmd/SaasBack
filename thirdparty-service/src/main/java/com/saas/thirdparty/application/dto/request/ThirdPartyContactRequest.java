package com.saas.thirdparty.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ThirdPartyContactRequest(
        @NotNull UUID thirdPartyId,
        @NotNull UUID contactTypeId,
        @NotBlank @Size(max = 160) String value,
        Boolean isPrimary,
        Boolean isVerified,
        @Size(max = 255) String notes
) {}
