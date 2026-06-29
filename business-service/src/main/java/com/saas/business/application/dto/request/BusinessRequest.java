package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BusinessRequest(
        @NotNull UUID businessTypeId,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 200) String legalName,
        @Size(max = 160) String tradeName,
        UUID documentTypeId,
        @Size(max = 40) String documentNumber,
        @Size(max = 500) String logoUrl,
        UUID statusId,
        @Size(max = 20) String primaryColor,
        @Size(max = 20) String secondaryColor
) {}
