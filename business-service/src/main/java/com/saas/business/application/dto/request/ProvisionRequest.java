package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Aprovisionamiento de un negocio nuevo (post-login del dueño). En una sola
 * llamada crea la empresa, su slug, la persona del dueño (en thirdparty) y el
 * vínculo business_owner al 100%.
 */
public record ProvisionRequest(
        // Empresa
        @NotNull UUID businessTypeId,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 200) String legalName,
        @Size(max = 160) String tradeName,
        UUID documentTypeId,
        @Size(max = 40) String documentNumber,
        @Size(max = 500) String logoUrl,
        @Size(max = 20) String primaryColor,
        @Size(max = 20) String secondaryColor,
        UUID statusId,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,63}$",
                message = "El slug admite minusculas, numeros y guiones (3-63)") String slug,

        // Dueño (persona natural en thirdparty)
        @NotNull UUID ownerDocumentTypeId,
        @NotBlank @Size(max = 40) String ownerDocumentNumber,
        @NotBlank @Size(max = 80) String ownerFirstName,
        @Size(max = 80) String ownerSecondName,
        @NotBlank @Size(max = 80) String ownerFirstLastName,
        @Size(max = 80) String ownerSecondLastName,
        UUID ownerGenderId,
        LocalDate ownerBirthDate,
        /** Cuenta de auth a la que se vincula la persona (opcional). */
        UUID ownerUserId
) {}
