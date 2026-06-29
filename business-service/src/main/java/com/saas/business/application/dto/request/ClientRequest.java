package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ClientRequest(
        @NotNull UUID thirdPartyId,
        UUID registrationStatusId,
        @Size(max = 80) String acquisitionSource,
        @Size(max = 255) String notes
) {}
