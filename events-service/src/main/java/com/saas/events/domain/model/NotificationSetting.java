package com.saas.events.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class NotificationSetting extends BaseDomain {
    private String apiKey;
    private String fromName;
    private String fromEmail;
    private String replyTo;
    private Boolean testMode;
    private String testRecipient;
    private Boolean sendingEnabled;
    private Integer maxRetries;
    private Integer retryDelaySeconds;
    private Integer logRetentionDays;
    private Integer monthlyQuota;
    private Integer quotaMonthlyUsed;
    private Integer quotaDailyUsed;
    private Integer rateLimit;
    private Integer rateRemaining;
    private LocalDateTime quotaObservedAt;
}
