package com.saas.events.domain.port.out;

import com.saas.events.domain.model.NotificationSetting;

import java.util.Optional;
import java.util.UUID;

public interface INotificationSettingRepositoryPort {

    Optional<NotificationSetting> findSingleton();

    NotificationSetting save(NotificationSetting setting);

    /** Id fijo de la unica fila; la siembra la migracion V1. */
    UUID SINGLETON_ID = UUID.fromString("99990000-0000-0000-0000-000000000001");
}
