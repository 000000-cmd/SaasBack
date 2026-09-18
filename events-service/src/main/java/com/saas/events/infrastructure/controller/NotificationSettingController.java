package com.saas.events.infrastructure.controller;

import com.saas.common.dto.ApiResponse;
import com.saas.events.application.dto.request.NotificationSettingRequest;
import com.saas.events.application.dto.response.NotificationSettingResponse;
import com.saas.events.application.service.NotificationSettingService;
import com.saas.events.domain.model.NotificationSetting;
import com.saas.events.domain.port.in.INotificationSettingUseCase;
import com.saas.events.infrastructure.client.ResendClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification/settings")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final INotificationSettingUseCase useCase;
    /** Solo para recordQuota(): no forma parte del puerto de entrada. */
    private final NotificationSettingService settingsService;
    private final ResendClient resendClient;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success(toResponse(useCase.get())));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> update(
            @Valid @RequestBody NotificationSettingRequest req) {
        NotificationSetting incoming = NotificationSetting.builder()
                .apiKey(req.apiKey())
                .fromName(req.fromName())
                .fromEmail(req.fromEmail())
                .replyTo(req.replyTo())
                .testMode(req.testMode())
                .testRecipient(req.testRecipient())
                .sendingEnabled(req.sendingEnabled())
                .maxRetries(req.maxRetries())
                .retryDelaySeconds(req.retryDelaySeconds())
                .logRetentionDays(req.logRetentionDays())
                .monthlyQuota(req.monthlyQuota())
                .build();
        return ResponseEntity.ok(ApiResponse.success(toResponse(useCase.update(incoming))));
    }

    /**
     * Llamada de baja escritura (una lectura a /domains) que sirve para validar
     * la clave y refrescar el retrato de cuota sin gastar un envio real.
     */
    @PostMapping("/test-connection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestConnectionResponse>> testConnection() {
        NotificationSetting setting = useCase.get();
        ResendClient.SendResult r = resendClient.probe(setting.getApiKey());
        if (r.quota() != null) {
            settingsService.recordQuota(r.quota().monthlyUsed(), r.quota().dailyUsed(),
                    r.quota().rateLimit(), r.quota().rateRemaining());
        }
        return ResponseEntity.ok(ApiResponse.success(new TestConnectionResponse(r.ok(), r.error())));
    }

    private static NotificationSettingResponse toResponse(NotificationSetting s) {
        return new NotificationSettingResponse(
                NotificationSettingResponse.mask(s.getApiKey()),
                s.getApiKey() != null && !s.getApiKey().isBlank(),
                s.getFromName(),
                s.getFromEmail(),
                s.getReplyTo(),
                s.getTestMode(),
                s.getTestRecipient(),
                s.getSendingEnabled(),
                s.getMaxRetries(),
                s.getRetryDelaySeconds(),
                s.getLogRetentionDays(),
                s.getMonthlyQuota(),
                s.getQuotaMonthlyUsed(),
                s.getQuotaDailyUsed(),
                s.getRateLimit(),
                s.getRateRemaining(),
                s.getQuotaObservedAt());
    }

    public record TestConnectionResponse(boolean ok, String error) {}
}
