package com.saas.events.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.events.domain.model.Notification;
import com.saas.events.domain.port.out.INotificationRepositoryPort;
import com.saas.events.infrastructure.persistence.entity.NotificationEntity;
import com.saas.events.infrastructure.persistence.mapper.NotificationPersistenceMapper;
import com.saas.events.infrastructure.persistence.repository.JpaNotificationRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Notification, NotificationEntity, UUID>
        implements INotificationRepositoryPort {

    private final JpaNotificationRepository jpa;

    public NotificationRepositoryAdapter(JpaNotificationRepository jpa,
                                         NotificationPersistenceMapper mapper) {
        super(jpa, mapper, "Notification");
        this.jpa = jpa;
    }

    @Override
    public Optional<Notification> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
