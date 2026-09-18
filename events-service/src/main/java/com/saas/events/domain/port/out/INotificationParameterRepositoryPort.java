package com.saas.events.domain.port.out;

import com.saas.common.port.out.ICodeRepositoryPort;
import com.saas.events.domain.model.NotificationParameter;

import java.util.UUID;

public interface INotificationParameterRepositoryPort
        extends ICodeRepositoryPort<NotificationParameter, UUID> {
}
