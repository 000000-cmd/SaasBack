package com.saas.events.domain.port.in;

import com.saas.common.port.in.ICodeUseCase;
import com.saas.events.domain.model.NotificationParameter;

import java.util.UUID;

public interface INotificationParameterUseCase
        extends ICodeUseCase<NotificationParameter, UUID> {
}
