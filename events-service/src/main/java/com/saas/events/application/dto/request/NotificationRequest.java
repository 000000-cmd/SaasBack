package com.saas.events.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationRequest(
        @NotBlank @Size(max = 80)  String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500)           String description,
        /** Marca la notificacion como lanzable a toda la base. */
        Boolean isGlobal
) {}
