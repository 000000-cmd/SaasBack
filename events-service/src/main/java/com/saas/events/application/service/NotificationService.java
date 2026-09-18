package com.saas.events.application.service;

import com.saas.common.service.CodeCrudService;
import com.saas.events.domain.model.Notification;
import com.saas.events.domain.port.in.INotificationUseCase;
import com.saas.events.domain.port.out.INotificationRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService
        extends CodeCrudService<Notification, UUID>
        implements INotificationUseCase {

    public NotificationService(INotificationRepositoryPort repo) { super(repo); }

    @Override protected String getResourceName() { return "Notificación"; }

    @Override
    protected void applyChanges(Notification existing, Notification incoming) {
        if (incoming.getCode() != null)        existing.setCode(incoming.getCode());
        if (incoming.getName() != null)        existing.setName(incoming.getName());
        if (incoming.getDescription() != null) existing.setDescription(incoming.getDescription());
        if (incoming.getIsGlobal() != null)     existing.setIsGlobal(incoming.getIsGlobal());
    }
}
