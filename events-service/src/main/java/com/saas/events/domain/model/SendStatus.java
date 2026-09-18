package com.saas.events.domain.model;

/**
 * Resultado de un intento de envio.
 *   SENT        entregado al proveedor, con su id de mensaje.
 *   FAILED      el proveedor rechazo o fallo la llamada.
 *   SKIPPED     no se intento (envio apagado, sin destinatario, plantilla inactiva).
 *   NO_PROVIDER el canal existe pero todavia no tiene proveedor conectado.
 */
public enum SendStatus {
    SENT, FAILED, SKIPPED, NO_PROVIDER
}
