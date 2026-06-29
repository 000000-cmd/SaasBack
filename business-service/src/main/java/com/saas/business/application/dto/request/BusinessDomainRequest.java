package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BusinessDomainRequest(
        @NotNull UUID businessId,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,63}$",
                message = "El slug admite minusculas, numeros y guiones (3-63)") String slug,
        @Size(max = 255) String customDomain,
        Boolean isPrimary,
        UUID statusId
) {}
