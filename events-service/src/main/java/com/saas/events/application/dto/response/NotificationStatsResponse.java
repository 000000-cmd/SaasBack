package com.saas.events.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record NotificationStatsResponse(
        long today,
        long month,
        long total,
        Map<String, Long> byStatus,
        List<DayCount> daily,
        Quota quota,
        Rate rate,
        LocalDateTime observedAt,
        List<Failure> lastFailures
) {
    public record DayCount(String date, long sent, long failed) {}
    /** monthlyLimit lo escribe el administrador: Resend no publica el tope del plan por API. */
    public record Quota(Integer monthlyUsed, Integer monthlyLimit, Integer dailyUsed) {}
    public record Rate(Integer limit, Integer remaining) {}
    public record Failure(String recipient, String error, LocalDateTime at) {}
}
