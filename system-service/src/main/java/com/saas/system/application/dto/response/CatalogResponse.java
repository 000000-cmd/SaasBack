package com.saas.system.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response generico para items de cualquier catalogo. Se construye desde
 * {@link com.saas.common.model.BaseCatalogDomain}.
 */
public record CatalogResponse(
        UUID id,
        String code,
        String name,
        String value,
        Integer displayOrder,
        Boolean enabled,
        Boolean visible,
        LocalDateTime createdDate,
        LocalDateTime auditDate
) {}
