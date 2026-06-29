package com.saas.business.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OfferingResponse(
        UUID id, UUID businessId, UUID categoryId, String name, String description,
        Integer durationMinutes, BigDecimal price, Boolean isActive, Boolean enabled
) {}
