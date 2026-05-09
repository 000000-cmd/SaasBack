package com.saas.search.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Almacen distribuido de IDs de eventos ya procesados (idempotencia).
 *
 * <p>Cuando llega un evento del topic, se intenta reservar su {@code eventId}
 * en Redis ({@code SET key value EX 86400 NX} - atomico). Si la key ya existia,
 * el evento es duplicado y se descarta.
 *
 * <p>TTL de 24h: ventana de "deduplicacion" suficiente para retries de Kafka
 * (rebalance, network blips). Despues de 24h Redis libera la key automaticamente.
 *
 * <p><b>Distribuido:</b> al usar Redis (no caffeine en memoria), funciona aunque
 * tengas N instancias de search-service. Todas comparten el mismo "set procesado".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedEventCache {

    private final StringRedisTemplate redis;

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "evt:";

    /**
     * Marca un eventId como procesado, atomicamente.
     *
     * @return {@code true} si era la primera vez (procesar). {@code false} si
     *         ya estaba registrado (duplicado, descartar).
     */
    public boolean markIfFirst(UUID eventId) {
        String key = PREFIX + eventId;
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", TTL);
        boolean isFirst = Boolean.TRUE.equals(acquired);
        if (!isFirst) {
            log.debug("Evento duplicado descartado: eventId={}", eventId);
        }
        return isFirst;
    }
}