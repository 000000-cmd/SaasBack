package com.saas.events.application.dto.request;

import jakarta.validation.constraints.Size;

/**
 * PUT parcial: todos los campos son opcionales. {@code apiKey} nulo o
 * enmascarado (contiene "•") deja la clave real intacta, lo resuelve
 * {@code NotificationSettingService.update}.
 */
public record NotificationSettingRequest(
        @Size(max = 200) String apiKey,
        @Size(max = 120) String fromName,
        @Size(max = 200) String fromEmail,
        @Size(max = 200) String replyTo,
        Boolean testMode,
        @Size(max = 200) String testRecipient,
        Boolean sendingEnabled,
        Integer maxRetries,
        Integer retryDelaySeconds,
        Integer logRetentionDays,
        Integer monthlyQuota
) {}
