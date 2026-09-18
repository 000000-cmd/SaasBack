package com.saas.events.application.dto.response;

import java.time.LocalDateTime;

public record NotificationSettingResponse(
        /** Enmascarada: re_****1a2b. La clave real NUNCA sale del backend. */
        String apiKey,
        Boolean apiKeyConfigured,
        String fromName,
        String fromEmail,
        String replyTo,
        Boolean testMode,
        String testRecipient,
        Boolean sendingEnabled,
        Integer maxRetries,
        Integer retryDelaySeconds,
        Integer logRetentionDays,
        Integer monthlyQuota,
        Integer quotaMonthlyUsed,
        Integer quotaDailyUsed,
        Integer rateLimit,
        Integer rateRemaining,
        LocalDateTime quotaObservedAt
) {
    /** Deja ver el prefijo y los cuatro ultimos: suficiente para identificarla, inutil para usarla. */
    public static String mask(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (raw.length() <= 8) return "••••";
        return raw.substring(0, 3) + "••••" + raw.substring(raw.length() - 4);
    }
}
