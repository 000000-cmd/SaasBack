package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.domain.model.AppointmentStatus;
import com.saas.business.infrastructure.persistence.entity.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaAppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {

    Optional<AppointmentEntity> findByPublicCode(String publicCode);

    /**
     * Las citas de un empleado que SE SOLAPAN con un rango.
     *
     * <p>La condicion es {@code inicio < fin_pedido AND inicio_pedido < fin}, no
     * {@code <=}: dos citas pegadas —una acaba a las 10:30 y la otra empieza a
     * las 10:30— NO se solapan. Con {@code <=} se solaparian en un instante y
     * la agenda rechazaria la mitad de las reservas seguidas.</p>
     *
     * <p>Se excluyen las retroactivas: registrar algo ya prestado puede
     * convivir con lo que hubiera, y contarlas aqui bloquearia huecos futuros
     * por cosas del pasado.</p>
     */
    @Query("SELECT a FROM AppointmentEntity a "
         + "WHERE a.employeeId = :employeeId "
         + "  AND a.status IN :occupying "
         + "  AND a.backdated = false "
         + "  AND a.startUtc < :end AND :start < a.endUtc")
    List<AppointmentEntity> overlapping(@Param("employeeId") UUID employeeId,
                                        @Param("start") Instant start,
                                        @Param("end") Instant end,
                                        @Param("occupying") Collection<AppointmentStatus> occupying);

    /** La agenda de un dia, para el panel y para el APK. */
    @Query("SELECT a FROM AppointmentEntity a "
         + "WHERE a.businessId = :businessId AND a.localDate = :day "
         + "ORDER BY a.startUtc ASC")
    List<AppointmentEntity> byBusinessAndDay(@Param("businessId") UUID businessId,
                                             @Param("day") LocalDate day);

    /** Solo las de un empleado: es lo unico que ve su app. */
    @Query("SELECT a FROM AppointmentEntity a "
         + "WHERE a.employeeId = :employeeId AND a.localDate BETWEEN :from AND :to "
         + "ORDER BY a.startUtc ASC")
    List<AppointmentEntity> byEmployeeBetween(@Param("employeeId") UUID employeeId,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);

    /**
     * Lo que ocupa agenda en un rango, para alimentar el motor de
     * disponibilidad. Las retroactivas quedan fuera por el mismo motivo.
     */
    @Query("SELECT a FROM AppointmentEntity a "
         + "WHERE a.businessId = :businessId "
         + "  AND a.status IN :occupying "
         + "  AND a.backdated = false "
         + "  AND a.startUtc < :end AND :start < a.endUtc")
    List<AppointmentEntity> busyInRange(@Param("businessId") UUID businessId,
                                        @Param("start") Instant start,
                                        @Param("end") Instant end,
                                        @Param("occupying") Collection<AppointmentStatus> occupying);

    /** Las pendientes que ya se pasaron de plazo. Las expira un proceso. */
    @Query("SELECT a FROM AppointmentEntity a "
         + "WHERE a.status = com.saas.business.domain.model.AppointmentStatus.PENDIENTE_CONFIRMACION "
         + "  AND a.createdDate < :cutoff")
    List<AppointmentEntity> pendingOlderThan(@Param("cutoff") java.time.LocalDateTime cutoff);

    /**
     * Las citas EN PIE que empiezan dentro de un rango. Es lo que barre el
     * recordatorio.
     *
     * <p>Solo confirmadas: recordar algo que el negocio todavia no ha aceptado
     * seria prometer una hora que quiza no exista. Y sin retroactivas, que ya
     * pasaron.</p>
     */
    @Query("SELECT a FROM AppointmentEntity a "
         + "WHERE a.status = com.saas.business.domain.model.AppointmentStatus.CONFIRMADA "
         + "  AND a.backdated = false "
         + "  AND a.startUtc >= :from AND a.startUtc < :to "
         + "ORDER BY a.startUtc ASC")
    List<AppointmentEntity> confirmedStartingBetween(@Param("from") Instant from,
                                                     @Param("to") Instant to);

    boolean existsByPublicCode(String publicCode);
}
