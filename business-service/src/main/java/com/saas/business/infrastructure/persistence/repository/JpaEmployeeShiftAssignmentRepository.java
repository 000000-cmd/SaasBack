package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.EmployeeShiftAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaEmployeeShiftAssignmentRepository extends JpaRepository<EmployeeShiftAssignmentEntity, UUID> {
    List<EmployeeShiftAssignmentEntity> findByEmployeeId(UUID employeeId);
    List<EmployeeShiftAssignmentEntity> findByEmployeeIdAndValidToIsNull(UUID employeeId);

    /**
     * Las asignaciones de varios empleados VIGENTES EN UN RANGO, en una consulta.
     *
     * <p>No sirve filtrar por {@code ValidTo IS NULL}: una asignacion con fecha
     * de fin puesta para dentro de un mes sigue valiendo hoy, y el calendario
     * de las proximas semanas la necesita. Lo que se pide es solapamiento con
     * el rango consultado.</p>
     */
    @Query("SELECT a FROM EmployeeShiftAssignmentEntity a "
         + "WHERE a.employeeId IN :employeeIds AND a.enabled = true "
         + "AND a.validFrom <= :to AND (a.validTo IS NULL OR a.validTo >= :from)")
    List<EmployeeShiftAssignmentEntity> effectiveIn(@Param("employeeIds") Collection<UUID> employeeIds,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);
}
