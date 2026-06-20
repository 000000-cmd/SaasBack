package com.saas.audit.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Idempotencia distribuida via Redis. Reserva atomica del eventId
 * ({@code SET key 1 EX 86400 NX}). Si la key existia, es duplicado.
 *
 * Es una primera barrera barata; la unicidad de {@code EventId} en
 * {@code audit_log} es la garantia final a nivel BD.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedEventCache {

    private final StringRedisTemplate redis;
    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "audit:evt:";

    public boolean markIfFirst(UUID eventId) {
        String key = PREFIX + eventId;
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", TTL);
        boolean isFirst = Boolean.TRUE.equals(acquired);
        if (!isFirst) log.debug("Evento de auditoria duplicado descartado: eventId={}", eventId);
        return isFirst;
    }
}
