package com.saas.events.application.service;

import com.saas.events.domain.port.out.INotificationLogRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Sin esto, notification_log crece sin fin y el ajuste de retencion seria
 * decorativo. Requiere @EnableScheduling en EventsServiceApplication (Fase A):
 * sin esa anotacion, @Scheduled se ignora EN SILENCIO.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogPurgeJob {

    private final INotificationLogRepositoryPort logs;
    private final NotificationSettingService settings;

    @Scheduled(cron = "0 30 3 * * *")   // 03:30 cada dia
    @Transactional
    public void purge() {
        Integer days = settings.get().getLogRetentionDays();
        if (days == null || days <= 0) return;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        long removed = logs.deleteOlderThan(cutoff);
        if (removed > 0) log.info("Bitacora de notificaciones: {} filas purgadas (> {} dias)", removed, days);
    }
}
