package com.saas.thirdparty.application.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload de creacion/actualizacion de un tercero (persona natural).
 *
 * <p>Documento y nombres son OPCIONALES: el alta minima de empleado crea un
 * tercero "shell" (solo userId+businessId) y el propio empleado completa sus
 * datos desde el APK. La unicidad del documento se valida cuando viene.</p>
 */
public record ThirdPartyRequest(

        UUID documentTypeId,

        @Size(max = 40) String documentNumber,

        UUID userId,

        @Nullable
        UUID businessId,

        @Size(max = 80) String firstName,
        @Size(max = 80) String secondName,
        @Size(max = 80) String firstLastName,
        @Size(max = 80) String secondLastName,

        UUID genderId,
        LocalDate birthDate,
        @Size(max = 500) String photoUrl
) {}
