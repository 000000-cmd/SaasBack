package com.saas.events.infrastructure.persistence.adapter;

import com.saas.common.dto.PagedResponse;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.events.domain.model.NotificationInbox;
import com.saas.events.domain.port.out.INotificationInboxRepositoryPort;
import com.saas.events.infrastructure.persistence.entity.NotificationInboxEntity;
import com.saas.events.infrastructure.persistence.mapper.NotificationInboxPersistenceMapper;
import com.saas.events.infrastructure.persistence.repository.JpaNotificationInboxRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class NotificationInboxRepositoryAdapter
        extends BaseJpaRepositoryAdapter<NotificationInbox, NotificationInboxEntity, UUID>
        implements INotificationInboxRepositoryPort {

    private final JpaNotificationInboxRepository jpa;

    public NotificationInboxRepositoryAdapter(JpaNotificationInboxRepository jpa,
                                              NotificationInboxPersistenceMapper mapper) {
        super(jpa, mapper, "NotificationInbox");
        this.jpa = jpa;
    }

    @Override
    public PagedResponse<NotificationInbox> findForOwner(UUID thirdPartyId, int page, int size) {
        Page<NotificationInboxEntity> p =
                jpa.findByThirdPartyIdOrderByCreatedDateDesc(thirdPartyId, PageRequest.of(page, size));
        return PagedResponse.of(
                p.getContent().stream().map(getMapper()::toDomain).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements());
    }

    @Override
    public long countUnread(UUID thirdPartyId) {
        return jpa.countByThirdPartyIdAndReadAtIsNull(thirdPartyId);
    }

    @Override
    @Transactional
    public int markAllRead(UUID thirdPartyId) {
        return jpa.markAllRead(thirdPartyId, LocalDateTime.now());
    }
}
