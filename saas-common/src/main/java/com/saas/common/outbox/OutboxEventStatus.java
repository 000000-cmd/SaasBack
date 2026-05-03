package com.saas.common.outbox;

/**
 * Estado del envio de un evento del outbox a Kafka.
 *
 *   {@link #PENDING}: recien creado, esperando publicacion.
 *   {@link #PUBLISHED}: enviado a Kafka exitosamente.
 *   {@link #FAILED}: tras N reintentos fallidos, marcado como muerto.
 *       Requiere intervencion manual (reset a PENDING despues de
 *       investigar la causa raiz).
 *
 */
public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
