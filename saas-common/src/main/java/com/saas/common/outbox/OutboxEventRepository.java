package com.saas.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para {@link OutboxEvent}.
 *
 *  de {@link JpaRepository} los CRUD basicos: {@code save}, {@code findById},
 * {@code delete}, etc. Definimos solo el query custom que necesita el
 * {@code OutboxRelay}: sacar un batch de PENDING con lock pesimista
 * y SKIP LOCKED para permitir paralelismo.
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Toma un batch de eventos PENDING bloqueandolos para escritura.
     *
     *   {@code FOR UPDATE}: bloquea las filas para que otra transaccion
     *       no las lea (sino: 2 instancias del relay procesarian el mismo
     *       evento → duplicacion).
     *   {@code SKIP LOCKED}: si una fila ya esta bloqueada por otro
     *       relay corriendo en paralelo, la SALTA en lugar de esperar.
     *       Permite escalar horizontalmente: N instancias del relay procesan
     *       eventos distintos sin pisarse.
     *   El timeout de lock se pone en 0 para que falle rapido si hay
     *       contencion (en lugar de quedarse 50 segundos colgado).
     *
     * Requiere MySQL 8.0+ para soportar {@code SKIP LOCKED} (tu schema esta
     * en MySQL 8.4, perfecto).
     */
    @Query(value = """
        SELECT * FROM outbox_event
        WHERE Status = 'PENDING'
        ORDER BY CreatedAt ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> lockBatch(@Param("limit") int limit);
}
