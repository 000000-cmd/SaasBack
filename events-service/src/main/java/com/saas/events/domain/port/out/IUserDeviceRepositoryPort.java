package com.saas.events.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.events.domain.model.UserDevice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserDeviceRepositoryPort extends IGenericRepositoryPort<UserDevice, UUID> {
    Optional<UserDevice> findByToken(String fcmToken);
    List<UserDevice> findByOwner(UUID thirdPartyId);
    /** Se llama cuando el proveedor declara el token invalido. */
    void deleteByToken(String fcmToken);
}
