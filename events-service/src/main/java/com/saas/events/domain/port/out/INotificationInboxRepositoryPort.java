package com.saas.events.domain.port.out;

import com.saas.common.dto.PagedResponse;
import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.events.domain.model.NotificationInbox;

import java.util.UUID;

public interface INotificationInboxRepositoryPort extends IGenericRepositoryPort<NotificationInbox, UUID> {

    PagedResponse<NotificationInbox> findForOwner(UUID thirdPartyId, int page, int size);

    /** Lo que pinta el globito de la campana. */
    long countUnread(UUID thirdPartyId);

    /** Marca todas las no leidas de una persona. Devuelve cuantas cambio. */
    int markAllRead(UUID thirdPartyId);
}
