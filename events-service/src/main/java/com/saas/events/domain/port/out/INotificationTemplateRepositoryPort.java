package com.saas.events.domain.port.out;

import com.saas.common.port.out.ICodeRepositoryPort;
import com.saas.events.domain.model.NotificationTemplate;

import java.util.List;
import java.util.UUID;

public interface INotificationTemplateRepositoryPort
        extends ICodeRepositoryPort<NotificationTemplate, UUID> {

    /** Plantillas asociadas a una notificacion. Vacio si aun no se asocio ninguna. */
    List<NotificationTemplate> findByNotificationId(UUID notificationId);

    /** Plantillas sin notificacion asociada: las que ofrece el selector del panel. */
    List<NotificationTemplate> findUnassigned();
}
