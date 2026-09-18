package com.saas.auth.domain.port.out;

import com.saas.auth.domain.model.UserDeviceLink;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserDeviceLinkRepositoryPort extends IGenericRepositoryPort<UserDeviceLink, UUID> {

    /** El vinculo de este par usuario+aparato, este activo o revocado. */
    Optional<UserDeviceLink> find(UUID userId, String deviceId);

    /** Vinculos ACTIVOS de una cuenta: desde donde esta entrando. */
    List<UserDeviceLink> activeOfUser(UUID userId);

    /** Vinculos ACTIVOS de un aparato: que cuenta hay abierta ahi. */
    List<UserDeviceLink> activeOfDevice(String deviceId);

    /** Revoca un vinculo concreto. Devuelve cuantas filas cambiaron. */
    int revoke(UUID id, UUID revokedBy);
}
