package com.saas.events.infrastructure.channel;

import com.saas.events.application.service.NotificationSettingService;
import com.saas.events.domain.model.Attachment;
import com.saas.events.domain.model.ChannelType;
import com.saas.events.domain.model.NotificationSetting;
import com.saas.events.domain.model.SendStatus;
import com.saas.events.infrastructure.client.ResendClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmailChannelSender implements ChannelSender {

    private final ResendClient resend;
    private final NotificationSettingService settingsService;

    @Override
    public boolean supports(ChannelType type) { return type == ChannelType.EMAIL; }

    @Override
    public Outcome send(NotificationSetting s, String recipient, String subject,
                        String body, Attachment attachment, UUID businessId) {
        if (s.getApiKey() == null || s.getApiKey().isBlank()) {
            return new Outcome(SendStatus.SKIPPED, null, "Sin API key configurada");
        }
        if (s.getFromEmail() == null || s.getFromEmail().isBlank()) {
            return new Outcome(SendStatus.SKIPPED, null, "Sin remitente configurado");
        }

        String from = (s.getFromName() == null || s.getFromName().isBlank())
                ? s.getFromEmail()
                : s.getFromName() + " <" + s.getFromEmail() + ">";

        ResendClient.SendResult r = resend.send(ResendClient.SendCommand.builder()
                .apiKey(s.getApiKey())
                .from(from)
                .to(List.of(recipient))
                .replyTo(s.getReplyTo())
                .subject(subject)
                .html(body)
                .attachment(attachment != null && attachment.isUsable() ? attachment : null)
                .build());

        if (r.quota() != null) {
            settingsService.recordQuota(r.quota().monthlyUsed(), r.quota().dailyUsed(),
                    r.quota().rateLimit(), r.quota().rateRemaining());
        }

        return r.ok()
                ? new Outcome(SendStatus.SENT, r.messageId(), null)
                : new Outcome(SendStatus.FAILED, null, r.error());
    }
}
