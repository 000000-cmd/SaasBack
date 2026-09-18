package com.saas.events.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.events.domain.model.NotificationTemplate;
import com.saas.events.domain.port.out.INotificationTemplateRepositoryPort;
import com.saas.events.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.saas.events.infrastructure.persistence.mapper.NotificationTemplatePersistenceMapper;
import com.saas.events.infrastructure.persistence.repository.JpaNotificationTemplateRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationTemplateRepositoryAdapter
        extends BaseJpaRepositoryAdapter<NotificationTemplate, NotificationTemplateEntity, UUID>
        implements INotificationTemplateRepositoryPort {

    private final JpaNotificationTemplateRepository jpa;

    public NotificationTemplateRepositoryAdapter(JpaNotificationTemplateRepository jpa,
                                                  NotificationTemplatePersistenceMapper mapper) {
        super(jpa, mapper, "NotificationTemplate");
        this.jpa = jpa;
    }

    @Override
    public Optional<NotificationTemplate> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }

    @Override
    public List<NotificationTemplate> findByNotificationId(UUID notificationId) {
        return getMapper().toDomainList(jpa.findByNotificationId(notificationId));
    }

    @Override
    public List<NotificationTemplate> findUnassigned() {
        return getMapper().toDomainList(jpa.findByNotificationIdIsNull());
    }
}
