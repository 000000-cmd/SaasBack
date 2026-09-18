package com.saas.business.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @param phone      en E.164, tal y como esta guardado.
 * @param phoneLocal el mismo numero como se lee ("300 123 4567"), para no
 *                   obligar a cada pantalla a formatearlo por su cuenta.
 * @param walkIn     cliente de mostrador: sin persona detras y sin telefono.
 *                   La interfaz lo necesita para no ofrecerle "enviar
 *                   recordatorio" a alguien a quien no se le puede escribir.
 */
public record BusinessClientResponse(
        UUID id,
        UUID businessId,
        UUID thirdPartyId,
        String displayName,
        String phone,
        String phoneLocal,
        boolean phoneVerified,
        boolean walkIn,
        boolean whatsappAllowed,
        String acquisitionSource,
        String notes,
        Integer visitCount,
        Integer noShowCount,
        LocalDateTime lastVisitAt,
        Boolean enabled,
        Boolean visible,
        LocalDateTime createdDate,
        LocalDateTime auditDate
) {}
