package com.saas.thirdparty.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Vista de lectura de un tercero. {@code fullName} es derivado. */
public record ThirdPartyResponse(
        UUID id,
        UUID documentTypeId,
        String documentNumber,
        UUID userId,
        String firstName,
        String secondName,
        String firstLastName,
        String secondLastName,
        String fullName,
        UUID genderId,
        LocalDate birthDate,
        String photoUrl,
        Boolean biometricEnabled,
        Boolean enabled,
        Boolean visible,
        LocalDateTime createdDate,
        LocalDateTime auditDate
) {}
