package com.saas.system.application.dto.response;

/**
 * Contrato PÚBLICO de la versión vigente del APK. {@code downloadPath} es
 * relativo al gateway: cada cliente lo prefija con su base conocida, así el
 * mismo payload sirve en cualquier entorno.
 */
public record LatestAppResponse(
        String version,
        Integer versionCode,
        String notes,
        String checksum,
        Long sizeBytes,
        String downloadPath
) {}
