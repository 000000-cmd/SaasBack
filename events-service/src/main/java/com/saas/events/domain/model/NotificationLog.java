package com.saas.events.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class NotificationLog extends BaseDomain {
    private String notificationCode;
    private String templateCode;
    private UUID templateId;
    private String typeCode;
    private String recipient;
    private String subject;
    private SendStatus status;
    private String providerMessageId;
    private String error;
    private UUID eventId;
    private Integer attemptCount;
    private LocalDateTime sentAt;
}
