package com.saas.system.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.AppVersion;

import java.util.Optional;
import java.util.UUID;

public interface IAppVersionRepositoryPort extends IGenericRepositoryPort<AppVersion, UUID> {

    Optional<AppVersion> findCurrent();

    Optional<AppVersion> findByVersion(String version);

    boolean existsByVersionCode(Integer versionCode);

    /**
     * Borrado FÍSICO. Las versiones tienen UNIQUE (Version, VersionCode): un
     * soft-delete dejaría fantasmas que impiden re-subir la misma versión.
     */
    void hardDeleteById(UUID id);
}
