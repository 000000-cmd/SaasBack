package com.saas.events.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "notification_setting")
public class NotificationSettingEntity extends BaseEntity {

    @Column(name = "ApiKey", length = 200)
    private String apiKey;

    @Column(name = "FromName", length = 120)
    private String fromName;

    @Column(name = "FromEmail", length = 200)
    private String fromEmail;

    @Column(name = "ReplyTo", length = 200)
    private String replyTo;

    @Column(name = "TestMode", nullable = false)
    private Boolean testMode;

    @Column(name = "TestRecipient", length = 200)
    private String testRecipient;

    @Column(name = "SendingEnabled", nullable = false)
    private Boolean sendingEnabled;

    @Column(name = "MaxRetries", nullable = false)
    private Integer maxRetries;

    @Column(name = "RetryDelaySeconds", nullable = false)
    private Integer retryDelaySeconds;

    @Column(name = "LogRetentionDays", nullable = false)
    private Integer logRetentionDays;

    @Column(name = "MonthlyQuota")
    private Integer monthlyQuota;

    @Column(name = "QuotaMonthlyUsed")
    private Integer quotaMonthlyUsed;

    @Column(name = "QuotaDailyUsed")
    private Integer quotaDailyUsed;

    @Column(name = "RateLimit")
    private Integer rateLimit;

    @Column(name = "RateRemaining")
    private Integer rateRemaining;

    @Column(name = "QuotaObservedAt")
    private LocalDateTime quotaObservedAt;
}
