package com.saas.events.domain.model;

/**
 * Un adjunto que viaja con la notificacion.
 *
 * <p>Llega YA CONSTRUIDO desde quien pide el envio (hoy: el extracto de nomina
 * que genera finance, cifrado con el documento del empleado). Este servicio no
 * lo abre ni lo entiende: no conoce ningun dominio, y esa es justo la razon por
 * la que anadir una notificacion nueva no toca su codigo.</p>
 *
 * @param filename      nombre con el que el destinatario lo vera.
 * @param contentBase64 el binario en base64, tal como lo pide Resend.
 */
public record Attachment(String filename, String contentBase64) {

    public boolean isUsable() {
        return filename != null && !filename.isBlank()
                && contentBase64 != null && !contentBase64.isBlank();
    }
}
