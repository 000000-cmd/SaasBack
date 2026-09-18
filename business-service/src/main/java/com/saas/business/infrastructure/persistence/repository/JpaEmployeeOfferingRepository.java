package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.EmployeeOfferingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaEmployeeOfferingRepository extends JpaRepository<EmployeeOfferingEntity, UUID> {

    List<EmployeeOfferingEntity> findByEmployeeId(UUID employeeId);

    /**
     * Todas las asignaciones de un grupo de servicios.
     *
     * <p>Una sola consulta para resolver "quien puede atender esta cita": la
     * disponibilidad se pide con varios servicios a la vez y preguntarlo
     * servicio por servicio serian N viajes para un dato que cabe en uno.</p>
     */
    List<EmployeeOfferingEntity> findByOfferingIdIn(Collection<UUID> offeringIds);

    boolean existsByEmployeeIdAndOfferingId(UUID employeeId, UUID offeringId);

    /**
     * Deja la fila activa con estos valores, exista o este borrada.
     *
     * <p>Nativa a proposito. La clave unica es (EmployeeId, OfferingId) SIN
     * mirar Visible, asi que quitar un servicio y volver a ponerlo choca contra
     * el indice: el borrado es logico y la fila sigue ahi. Con JPA no se ve,
     * porque {@code @SQLRestriction} la filtra en toda consulta.</p>
     *
     * <p>Devuelve 0 cuando no existia ninguna fila; entonces —y solo
     * entonces— hay que insertar.</p>
     */
    @Modifying
    @Query(value = "UPDATE employee_offering SET Visible = 1, Enabled = 1, "
                 + "DurationMinutes = :duration, CommissionRate = :rate, AuditDate = NOW(6) "
                 + "WHERE EmployeeId = :employeeId AND OfferingId = :offeringId",
           nativeQuery = true)
    int upsertActive(@Param("employeeId") String employeeId,
                     @Param("offeringId") String offeringId,
                     @Param("duration") Integer duration,
                     @Param("rate") BigDecimal rate);
}
