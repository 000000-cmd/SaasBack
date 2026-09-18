package com.saas.events.infrastructure.persistence.entity;

import com.saas.common.persistence.BaseEntity;
import com.saas.events.domain.model.SendStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "notification_log")
public class NotificationLogEntity extends BaseEntity {

    @Column(name = "NotificationCode", length = 80)
    private String notificationCode;

    @Column(name = "TemplateCode", length = 80)
    private String templateCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "TemplateId", length = 36)
    private UUID templateId;

    @Column(name = "TypeCode", nullable = false, length = 40)
    private String typeCode;

    @Column(name = "Recipient", nullable = false, length = 320)
    private String recipient;

    @Column(name = "Subject", length = 300)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private SendStatus status;

    @Column(name = "ProviderMessageId", length = 120)
    private String providerMessageId;

    @Column(name = "Error", length = 500)
    private String error;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "EventId", length = 36)
    private UUID eventId;

    @Column(name = "AttemptCount", nullable = false)
    private Integer attemptCount;

    @Column(name = "SentAt")
    private LocalDateTime sentAt;
}
