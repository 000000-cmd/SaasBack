package com.saas.events.domain.port.in;

import com.saas.common.port.in.ICodeUseCase;
import com.saas.events.domain.model.Notification;

import java.util.UUID;

public interface INotificationUseCase
        extends ICodeUseCase<Notification, UUID> {
}
