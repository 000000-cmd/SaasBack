package com.saas.events.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.events.domain.model.NotificationParameter;
import com.saas.events.domain.port.out.INotificationParameterRepositoryPort;
import com.saas.events.infrastructure.persistence.entity.NotificationParameterEntity;
import com.saas.events.infrastructure.persistence.mapper.NotificationParameterPersistenceMapper;
import com.saas.events.infrastructure.persistence.repository.JpaNotificationParameterRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationParameterRepositoryAdapter
        extends BaseJpaRepositoryAdapter<NotificationParameter, NotificationParameterEntity, UUID>
        implements INotificationParameterRepositoryPort {

    private final JpaNotificationParameterRepository jpa;

    public NotificationParameterRepositoryAdapter(JpaNotificationParameterRepository jpa,
                                                   NotificationParameterPersistenceMapper mapper) {
        super(jpa, mapper, "NotificationParameter");
        this.jpa = jpa;
    }

    @Override
    public Optional<NotificationParameter> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
