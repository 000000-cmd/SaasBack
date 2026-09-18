package com.saas.events.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record SendRequest(
        @NotBlank String notificationCode,
        @NotEmpty List<String> to,
        Map<String, String> data
) {}
