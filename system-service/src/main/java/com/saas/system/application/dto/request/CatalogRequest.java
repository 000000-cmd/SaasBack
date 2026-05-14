package com.saas.system.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload generico para crear/actualizar items de cualquier catalogo.
 * Todos los catalogos comparten esta misma estructura.
 */
public record CatalogRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String value,
        @NotNull Integer displayOrder
) {}
