package com.saas.events.application.dto.response;

import java.util.List;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String code,
        String name,
        String description,
        /** Codigos de canal de las plantillas asociadas: ["EMAIL","SMS"]. */
        List<String> channels,
        /** Si es lanzable a toda la base desde "Lanzamiento global". */
        Boolean isGlobal,
        Boolean enabled,
        Boolean visible
) {}
