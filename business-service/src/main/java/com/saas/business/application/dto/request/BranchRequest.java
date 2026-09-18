package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record BranchRequest(
        @NotNull UUID businessId,
        @NotNull UUID branchTypeId,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 40) String code,
        @NotNull UUID municipalityId,
        UUID neighborhoodId,
        @Size(max = 255) String addressLine,
        // Rango real de la Tierra. Sin esto, un dedo de mas en el teclado pone
        // el local en mitad del oceano y el mapa se va a otro continente.
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @Size(max = 30) String phone,
        Boolean isMain,
        UUID statusId
) {}
