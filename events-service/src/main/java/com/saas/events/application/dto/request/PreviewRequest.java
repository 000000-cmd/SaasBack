package com.saas.events.application.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record PreviewRequest(
        @NotBlank String typeCode,
        String subject,
        String body,
        Map<String, String> data
) {}
