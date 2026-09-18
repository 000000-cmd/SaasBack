package com.saas.events.infrastructure.persistence.adapter;

import com.saas.events.domain.model.NotificationSetting;
import com.saas.events.domain.port.out.INotificationSettingRepositoryPort;
import com.saas.events.infrastructure.persistence.mapper.NotificationSettingPersistenceMapper;
import com.saas.events.infrastructure.persistence.repository.JpaNotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * No extiende {@code BaseJpaRepositoryAdapter}: el puerto de la fila unica no
 * es el CRUD generico (sin findById/update por Id arbitrario), asi que el
 * adaptador es directo. {@code save()} hace un {@code toEntity()} completo
 * (sin merge parcial), que es justo lo que necesita el servicio para poder
 * escribir un campo a null si algun dia hiciera falta.
 */
@Repository
@RequiredArgsConstructor
public class NotificationSettingRepositoryAdapter implements INotificationSettingRepositoryPort {

    private final JpaNotificationSettingRepository jpa;
    private final NotificationSettingPersistenceMapper mapper;

    @Override
    public Optional<NotificationSetting> findSingleton() {
        return jpa.findById(SINGLETON_ID).map(mapper::toDomain);
    }

    @Override
    public NotificationSetting save(NotificationSetting setting) {
        return mapper.toDomain(jpa.save(mapper.toEntity(setting)));
    }
}
