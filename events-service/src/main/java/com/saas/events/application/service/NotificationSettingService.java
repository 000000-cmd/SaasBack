package com.saas.events.application.service;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.events.domain.model.NotificationSetting;
import com.saas.events.domain.port.in.INotificationSettingUseCase;
import com.saas.events.domain.port.out.INotificationSettingRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationSettingService implements INotificationSettingUseCase {

    private final INotificationSettingRepositoryPort repo;

    @Override
    @Transactional(readOnly = true)
    public NotificationSetting get() {
        return repo.findSingleton().orElseThrow(() ->
                new ResourceNotFoundException("Configuración de notificaciones", "Id",
                        INotificationSettingRepositoryPort.SINGLETON_ID.toString()));
    }

    /**
     * La clave del proveedor solo se sobrescribe si llega una nueva. El panel
     * devuelve la mascara (re_****1a2b) en el GET, asi que un PUT que la
     * reenvie tal cual NO debe destruir la clave real.
     */
    @Override
    @Transactional
    public NotificationSetting update(NotificationSetting incoming) {
        NotificationSetting current = get();
        if (incoming.getApiKey() != null && !incoming.getApiKey().isBlank()
                && !incoming.getApiKey().contains("•")) {
            current.setApiKey(incoming.getApiKey().trim());
        }
        if (incoming.getFromName() != null)          current.setFromName(incoming.getFromName());
        if (incoming.getFromEmail() != null)         current.setFromEmail(incoming.getFromEmail());
        if (incoming.getReplyTo() != null)           current.setReplyTo(incoming.getReplyTo());
        if (incoming.getTestMode() != null)          current.setTestMode(incoming.getTestMode());
        if (incoming.getTestRecipient() != null)     current.setTestRecipient(incoming.getTestRecipient());
        if (incoming.getSendingEnabled() != null)    current.setSendingEnabled(incoming.getSendingEnabled());
        if (incoming.getMaxRetries() != null)        current.setMaxRetries(incoming.getMaxRetries());
        if (incoming.getRetryDelaySeconds() != null) current.setRetryDelaySeconds(incoming.getRetryDelaySeconds());
        if (incoming.getLogRetentionDays() != null)  current.setLogRetentionDays(incoming.getLogRetentionDays());
        if (incoming.getMonthlyQuota() != null)      current.setMonthlyQuota(incoming.getMonthlyQuota());
        return repo.save(current);
    }

    /** Guarda el retrato de cuota que vino en las cabeceras del proveedor. */
    @Transactional
    public void recordQuota(Integer monthlyUsed, Integer dailyUsed, Integer limit, Integer remaining) {
        NotificationSetting s = get();
        if (monthlyUsed != null) s.setQuotaMonthlyUsed(monthlyUsed);
        if (dailyUsed != null)   s.setQuotaDailyUsed(dailyUsed);
        if (limit != null)       s.setRateLimit(limit);
        if (remaining != null)   s.setRateRemaining(remaining);
        s.setQuotaObservedAt(LocalDateTime.now());
        repo.save(s);
    }
}
