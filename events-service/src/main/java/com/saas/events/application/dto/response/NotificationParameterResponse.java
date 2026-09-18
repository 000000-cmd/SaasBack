package com.saas.events.application.dto.response;

import java.util.UUID;

public record NotificationParameterResponse(
        UUID id,
        String code,
        String name,
        String description,
        Boolean enabled,
        Boolean visible
) {}
