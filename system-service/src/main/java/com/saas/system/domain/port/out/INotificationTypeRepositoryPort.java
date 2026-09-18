package com.saas.system.domain.port.out;

import com.saas.common.port.out.ICatalogRepositoryPort;
import com.saas.system.domain.model.NotificationType;

import java.util.UUID;

public interface INotificationTypeRepositoryPort
        extends ICatalogRepositoryPort<NotificationType, UUID> {
}
