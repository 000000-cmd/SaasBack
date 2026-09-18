package com.saas.events.application.dto.response;

import java.util.List;

public record PreviewResponse(
        String subject,
        String body,
        List<String> missingParameters
) {}
