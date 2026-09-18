package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.NotificationType;
import com.saas.system.domain.port.out.INotificationTypeRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.NotificationTypeEntity;
import com.saas.system.infrastructure.persistence.mapper.NotificationTypePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaNotificationTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationTypeRepositoryAdapter
        extends BaseJpaRepositoryAdapter<NotificationType, NotificationTypeEntity, UUID>
        implements INotificationTypeRepositoryPort {

    private final JpaNotificationTypeRepository jpa;

    public NotificationTypeRepositoryAdapter(JpaNotificationTypeRepository jpa,
                                              NotificationTypePersistenceMapper mapper) {
        super(jpa, mapper, "Tipo de notificación");
        this.jpa = jpa;
    }

    @Override
    public Optional<NotificationType> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
