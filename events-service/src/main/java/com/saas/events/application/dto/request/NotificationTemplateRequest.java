package com.saas.events.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record NotificationTemplateRequest(
        @NotBlank @Size(max = 80)  String code,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40)  String typeCode,
        @Size(max = 300)           String subject,
        @NotBlank                  String body,
        /** Null mientras la plantilla no este asociada. */
        UUID notificationId
) {}
