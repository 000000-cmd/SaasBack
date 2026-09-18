package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.AgendaLockEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * El cerrojo que serializa las reservas de un mismo empleado en un mismo dia.
 *
 * <p>Toda reserva lo toma PRIMERO, y solo despues comprueba solapamiento e
 * inserta. Dos peticiones simultaneas al mismo hueco se ponen en fila aqui: la
 * segunda entra cuando la primera ya escribio, ve el solape y falla con
 * {@code SLOT_NO_DISPONIBLE}.</p>
 *
 * <h3>Por que una fila y no un SELECT FOR UPDATE sobre el rango de citas</h3>
 * <p>MySQL 8 en REPEATABLE READ si toma gap locks sobre un rango vacio, y en
 * teoria bastaria. Pero que los tome depende del plan que elija el optimizador
 * para ese indice y ese rango; un anti doble reserva que falla una vez de cada
 * mil es peor que uno explicito. La fila que se bloquea aqui SIEMPRE existe
 * (se crea al vuelo si hace falta), asi que no hay hueco sobre el que discutir.</p>
 *
 * <p>Si algun dia una prueba de concurrencia demuestra que el gap lock aguanta,
 * quitar esta tabla es mucho mas facil que anadirla despues.</p>
 */
@Repository
public interface JpaAgendaLockRepository extends JpaRepository<AgendaLockEntity, UUID> {

    /**
     * Toma el cerrojo. Bloquea hasta que quien lo tenga cierre su transaccion.
     *
     * <p>{@code PESSIMISTIC_WRITE} sobre una fila que existe es un bloqueo de
     * fila normal de InnoDB: predecible y sin depender de indices ni de
     * niveles de aislamiento.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM AgendaLockEntity l WHERE l.employeeId = :employeeId AND l.localDate = :day")
    Optional<AgendaLockEntity> lock(@Param("employeeId") UUID employeeId,
                                    @Param("day") LocalDate day);

    /** Sin bloquear: para saber si hay que crearla antes de intentar tomarla. */
    @Query("SELECT l FROM AgendaLockEntity l WHERE l.employeeId = :employeeId AND l.localDate = :day")
    Optional<AgendaLockEntity> find(@Param("employeeId") UUID employeeId,
                                    @Param("day") LocalDate day);

    /**
     * Crea la fila si no existe. {@code INSERT IGNORE} y no un try/catch.
     *
     * <p>Capturar la excepcion de clave duplicada NO sirve: para cuando llega,
     * la transaccion ya esta marcada para deshacerse, y al confirmarla Spring
     * lanza "Transaction silently rolled back because it has been marked as
     * rollback-only". El error real —"esa hora se acaba de ocupar"— se pierde
     * detras de ese, que no le dice nada a nadie.</p>
     *
     * <p>{@code INSERT IGNORE} convierte el choque en cero filas afectadas: sin
     * excepcion, sin transaccion envenenada, y con exactamente el mismo
     * resultado — la fila esta.</p>
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO agenda_lock "
                 + "(Id, EmployeeId, LocalDate, Enabled, Visible, AuditDate, CreatedDate) "
                 + "VALUES (:id, :employeeId, :day, 1, 1, NOW(6), NOW(6))",
           nativeQuery = true)
    int insertIfAbsent(@Param("id") String id,
                       @Param("employeeId") String employeeId,
                       @Param("day") LocalDate day);
}
