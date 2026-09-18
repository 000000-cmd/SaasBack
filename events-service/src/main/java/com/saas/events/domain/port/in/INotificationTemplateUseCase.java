package com.saas.events.domain.port.in;

import com.saas.common.port.in.ICodeUseCase;
import com.saas.events.domain.model.NotificationTemplate;

import java.util.UUID;

public interface INotificationTemplateUseCase
        extends ICodeUseCase<NotificationTemplate, UUID> {
}
