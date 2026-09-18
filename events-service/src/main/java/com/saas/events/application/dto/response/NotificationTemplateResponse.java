package com.saas.events.application.dto.response;

import java.util.UUID;

public record NotificationTemplateResponse(
        UUID id,
        String code,
        String name,
        String typeCode,
        String subject,
        String body,
        UUID notificationId,
        Boolean enabled,
        Boolean visible
) {}
