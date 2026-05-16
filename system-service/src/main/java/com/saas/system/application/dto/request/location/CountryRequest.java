package com.saas.system.application.dto.request.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CountryRequest(
        @NotBlank @Size(max = 10) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 200) String officialName,
        @Size(max = 10) String isoCode3,
        @Size(max = 10) String numericCode,
        @Size(max = 10) String phoneCode,
        @Size(max = 10) String currencyCode,
        @Size(max = 10) String currencySymbol,
        @Size(max = 50) String continent
) {}
