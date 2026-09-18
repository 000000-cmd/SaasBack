package com.saas.system.application.service;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.NotificationType;
import com.saas.system.domain.port.out.INotificationTypeRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationTypeService extends BaseCatalogService<NotificationType, UUID> {

    public NotificationTypeService(INotificationTypeRepositoryPort repository) {
        super(repository);
    }

    @Override
    protected String getResourceName() {
        return "Tipo de notificación";
    }

    @Override
    public String getCatalogPath() {
        return "notification-type";
    }

    @Override
    public NotificationType newInstance() {
        return new NotificationType();
    }
}
