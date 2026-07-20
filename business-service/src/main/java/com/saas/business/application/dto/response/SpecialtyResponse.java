package com.saas.business.application.dto.response;

import java.util.UUID;

public record SpecialtyResponse(
        UUID id, UUID businessId, String name, Integer displayOrder, Boolean enabled
) {}
