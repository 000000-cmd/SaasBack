package com.saas.thirdparty.application.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** Payload de creacion/actualizacion de un tercero (persona natural). */
public record ThirdPartyRequest(

        @NotNull UUID documentTypeId,

        @NotBlank @Size(max = 40) String documentNumber,

        UUID userId,

        @Nullable
        UUID businessId,

        @NotBlank @Size(max = 80) String firstName,
        @Size(max = 80) String secondName,
        @NotBlank @Size(max = 80) String firstLastName,
        @Size(max = 80) String secondLastName,

        UUID genderId,
        LocalDate birthDate,
        @Size(max = 500) String photoUrl
) {}
