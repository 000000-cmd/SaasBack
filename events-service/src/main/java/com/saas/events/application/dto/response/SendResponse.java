package com.saas.events.application.dto.response;

import com.saas.events.domain.model.NotificationLog;
import com.saas.events.domain.model.SendStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Vista de una entrada de bitacora recien escrita, sin los campos internos de BaseDomain. */
public record SendResponse(
        UUID id,
        String templateCode,
        String typeCode,
        String recipient,
        String subject,
        SendStatus status,
        String providerMessageId,
        String error,
        LocalDateTime sentAt
) {
    public static SendResponse from(NotificationLog log) {
        return new SendResponse(log.getId(), log.getTemplateCode(), log.getTypeCode(),
                log.getRecipient(), log.getSubject(), log.getStatus(),
                log.getProviderMessageId(), log.getError(), log.getSentAt());
    }
}
