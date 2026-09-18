package com.saas.events.domain.port.out;

import com.saas.common.port.out.ICodeRepositoryPort;
import com.saas.events.domain.model.Notification;

import java.util.UUID;

public interface INotificationRepositoryPort
        extends ICodeRepositoryPort<Notification, UUID> {
}
