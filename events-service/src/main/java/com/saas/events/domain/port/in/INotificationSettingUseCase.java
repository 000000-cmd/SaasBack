package com.saas.events.domain.port.in;

import com.saas.events.domain.model.NotificationSetting;

/**
 * Fila unica de configuracion de notificaciones. No es un catalogo por Id
 * como el resto de agregados: solo existe un registro, asi que el contrato
 * es distinto al CRUD generico ({@code IGenericUseCase}).
 */
public interface INotificationSettingUseCase {

    NotificationSetting get();

    NotificationSetting update(NotificationSetting incoming);
}
