package com.saas.events.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.service.CodeCrudService;
import com.saas.events.domain.model.ChannelType;
import com.saas.events.domain.model.NotificationTemplate;
import com.saas.events.domain.port.in.INotificationTemplateUseCase;
import com.saas.events.domain.port.out.INotificationTemplateRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationTemplateService
        extends CodeCrudService<NotificationTemplate, UUID>
        implements INotificationTemplateUseCase {

    public NotificationTemplateService(INotificationTemplateRepositoryPort repo) { super(repo); }

    @Override protected String getResourceName() { return "Plantilla"; }

    @Override
    protected void onBeforeCreate(NotificationTemplate entity) {
        validateType(entity.getTypeCode());
        super.onBeforeCreate(entity);
    }

    @Override
    protected void onBeforeUpdate(NotificationTemplate existing, NotificationTemplate incoming) {
        if (incoming.getTypeCode() != null) validateType(incoming.getTypeCode());
        super.onBeforeUpdate(existing, incoming);
    }

    private void validateType(String typeCode) {
        if (ChannelType.from(typeCode) == null) {
            throw new BusinessException("Tipo de plantilla desconocido: " + typeCode);
        }
    }

    @Override
    protected void applyChanges(NotificationTemplate existing, NotificationTemplate incoming) {
        if (incoming.getCode() != null)     existing.setCode(incoming.getCode());
        if (incoming.getName() != null)     existing.setName(incoming.getName());
        if (incoming.getTypeCode() != null) existing.setTypeCode(incoming.getTypeCode());
        if (incoming.getSubject() != null)  existing.setSubject(incoming.getSubject());
        if (incoming.getBody() != null)     existing.setBody(incoming.getBody());
        // notificationId NO entra aqui: desasociar es ponerlo a null, y el
        // patron de "solo si no es null" haria imposible expresarlo. Se maneja
        // con los endpoints dedicados de asociacion del controlador.
    }
}
