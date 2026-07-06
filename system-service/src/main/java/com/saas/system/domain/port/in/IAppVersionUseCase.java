package com.saas.system.domain.port.in;

import com.saas.common.port.in.IGenericUseCase;
import com.saas.system.domain.model.AppVersion;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

public interface IAppVersionUseCase extends IGenericUseCase<AppVersion, UUID> {

    /** Alta de una version: guarda el binario, calcula checksum y registra metadata. */
    AppVersion upload(String version, Integer versionCode, String notes, boolean publish, InputStream content);

    /** Marca la version como vigente (versionCode creciente) y sincroniza VERAPP. */
    AppVersion publish(UUID id);

    /** Version vigente (la que el APK exige y sirve el link publico). */
    Optional<AppVersion> findCurrent();

    /** Ruta absoluta del binario en disco para servir la descarga. */
    java.nio.file.Path binaryPath(AppVersion version);
}
