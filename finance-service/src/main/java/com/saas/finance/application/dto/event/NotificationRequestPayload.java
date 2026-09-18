package com.saas.finance.application.dto.event;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Peticion de notificacion publicada al outbox. La consume events-service, que
 * no conoce ningun dominio: recibe el codigo, los destinatarios y los valores de
 * los parametros, y resuelve el resto con sus propias plantillas.
 *
 * <p>Por eso anadir un correo nuevo no toca su codigo: se siembra la plantilla y
 * se publica este payload.</p>
 *
 * @param thirdPartyId de quien es la notificacion. Con el, ademas de enviarse,
 *        se le escribe en su bandeja (la campana de la web y la lista del movil).
 * @param attachment   fichero opcional ya construido y en base64.
 */
public record NotificationRequestPayload(
        String notificationCode,
        List<String> to,
        UUID thirdPartyId,
        Map<String, String> data,
        Attachment attachment
) {
    public record Attachment(String filename, String contentBase64) {}
}
