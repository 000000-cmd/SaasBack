package com.saas.events.application.service;

import com.saas.events.application.dto.response.NotificationStatsResponse;
import com.saas.events.domain.model.NotificationLog;
import com.saas.events.domain.model.NotificationSetting;
import com.saas.events.domain.model.SendStatus;
import com.saas.events.domain.port.out.INotificationLogRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compone el retrato de estadisticas que consume GET /notification/stats. */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final INotificationLogRepositoryPort logs;
    private final NotificationSettingService settingsService;

    @Transactional(readOnly = true)
    public NotificationStatsResponse get() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long today = logs.countSince(todayStart);
        long month = logs.countSince(monthStart);
        long total = logs.count();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (SendStatus s : SendStatus.values()) byStatus.put(s.name(), 0L);
        logs.countByStatus().forEach((status, count) -> byStatus.put(status.name(), count));

        List<NotificationStatsResponse.DayCount> daily = logs.dailySince(LocalDateTime.now().minusDays(30)).stream()
                .map(d -> new NotificationStatsResponse.DayCount(d.date().toString(), d.sent(), d.failed()))
                .toList();

        NotificationSetting settings = settingsService.get();
        NotificationStatsResponse.Quota quota = new NotificationStatsResponse.Quota(
                settings.getQuotaMonthlyUsed(), settings.getMonthlyQuota(), settings.getQuotaDailyUsed());
        NotificationStatsResponse.Rate rate = new NotificationStatsResponse.Rate(
                settings.getRateLimit(), settings.getRateRemaining());

        List<NotificationStatsResponse.Failure> lastFailures = logs.lastFailures().stream()
                .map(this::toFailure)
                .toList();

        return new NotificationStatsResponse(today, month, total, byStatus, daily, quota, rate,
                settings.getQuotaObservedAt(), lastFailures);
    }

    private NotificationStatsResponse.Failure toFailure(NotificationLog l) {
        return new NotificationStatsResponse.Failure(l.getRecipient(), l.getError(), l.getCreatedDate());
    }
}
