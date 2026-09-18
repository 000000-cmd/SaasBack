package com.saas.system.application.dto.request.flow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FlowMessageRequest(
        @NotBlank @Size(max = 60) String code,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 1000) String body,
        @Size(max = 300) String description,
        Boolean enabled
) {}
