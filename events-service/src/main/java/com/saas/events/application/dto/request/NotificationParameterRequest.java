package com.saas.events.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationParameterRequest(
        @NotBlank @Size(max = 80)  String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500)           String description
) {}
