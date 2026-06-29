package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record BranchRequest(
        @NotNull UUID businessId,
        @NotNull UUID branchTypeId,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 40) String code,
        @NotNull UUID municipalityId,
        UUID neighborhoodId,
        @Size(max = 255) String addressLine,
        @Size(max = 30) String phone,
        Boolean isMain,
        UUID statusId
) {}
