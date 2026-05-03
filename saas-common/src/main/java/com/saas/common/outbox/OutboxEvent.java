package com.saas.common.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code outbox_event}.
 *
 * Se inserta dentro de la transaccion del caso de uso para garantizar
 * atomicidad con el cambio de dominio. El {@code OutboxRelay} lee
 * los registros PENDING y los publica a Kafka.
 *
 * Compartida entre auth-service y system-service via {@code saas-common}.
 */
@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @Column(name = "Id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "EventId", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID eventId;

    @Column(name = "AggregateType", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "AggregateId", nullable = false, columnDefinition = "BINARY(16)")
    private UUID aggregateId;

    @Column(name = "EventType", nullable = false, length = 128)
    private String eventType;

    @Column(name = "Version", nullable = false)
    @Builder.Default
    private int version = 1;

    @Column(name = "BusinessId", columnDefinition = "BINARY(16)")
    private UUID businessId;

    /** Payload serializado a JSON (lo serializa el OutboxPublisher antes de salvar). */
    @Column(name = "Payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 16)
    @Builder.Default
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @Column(name = "Retries", nullable = false)
    @Builder.Default
    private int retries = 0;

    @Column(name = "LastError", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt;

    @Column(name = "PublishedAt")
    private Instant publishedAt;
}
