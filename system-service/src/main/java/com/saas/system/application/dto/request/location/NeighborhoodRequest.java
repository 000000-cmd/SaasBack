package com.saas.system.application.dto.request.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record NeighborhoodRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Pattern(regexp = "BARRIO|VEREDA|CORREGIMIENTO|OTRO") String type,
        @NotNull UUID municipalityId
) {}
