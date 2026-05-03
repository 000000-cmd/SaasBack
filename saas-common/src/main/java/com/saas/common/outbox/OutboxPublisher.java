package com.saas.common.outbox;

import java.util.UUID;

/**
 * API que usan los servicios para publicar eventos al outbox.
 *
 * Reglas de uso (CRITICAS):
 *
 *   Llamar SIEMPRE desde dentro de una transaccion ({@code @Transactional})
 *       junto con el cambio de dominio. Si el INSERT del dominio rollback,
 *       el evento NO se publica. Atomicidad garantizada por MySQL.
 *   El {@code payload} debe ser un DTO plano serializable por Jackson.
 *       NO pasar entidades JPA directamente (lazy-loading rompe serializacion).
 *   NUNCA poner contrasenas, tokens, hashes o secretos en el payload.
 *       Los eventos llegan a multiples consumers; lo que pongas se publica.
 *
 *
 * Ejemplo:
 * {@code
 * @Transactional
 * public User createUser(User u) {
 *     User saved = userRepo.save(u);
 *     outboxPublisher.publish(
 *         EventTypes.USER_CREATED,
 *         null,                       // businessId
 *         "user",                     // aggregateType
 *         saved.getId(),              // aggregateId
 *         UserEventPayload.from(saved)
 *     );
 *     return saved;
 * }
 * }
 */
public interface OutboxPublisher {

    /**
     * Encola un evento al outbox. Sera publicado a Kafka por el OutboxRelay.
     *
     * @param eventType     identificador del tipo de evento (usar constantes de
     *                      {@link com.saas.common.events.EventTypes}).
     * @param businessId    UUID del negocio dueno del cambio. Nullable.
     * @param aggregateType "user", "role", "menu", "payroll-run", ...
     * @param aggregateId   UUID de la entidad afectada.
     * @param payload       DTO plano con los datos del evento. Serializado a JSON.
     */
    void publish(String eventType,
                 UUID businessId,
                 String aggregateType,
                 UUID aggregateId,
                 Object payload);
}
