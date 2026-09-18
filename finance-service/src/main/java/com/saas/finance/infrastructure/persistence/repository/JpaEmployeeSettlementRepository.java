package com.saas.finance.infrastructure.persistence.repository;

import com.saas.finance.domain.model.MovementType;
import com.saas.finance.infrastructure.persistence.entity.EmployeeSettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaEmployeeSettlementRepository extends JpaRepository<EmployeeSettlementEntity, UUID> {
    List<EmployeeSettlementEntity> findByEmployeeIdOrderBySettledAtDesc(UUID employeeId);
    List<EmployeeSettlementEntity> findByBusinessIdOrderBySettledAtDesc(UUID businessId);

    /** Los movimientos de una corrida: es el detalle del historial de nomina. */
    List<EmployeeSettlementEntity> findByPayrollRunIdOrderBySettledAtDesc(UUID payrollRunId);

    /**
     * Si el sueldo base de ese periodo ya se abono. La unica de BD es la
     * garantia real; esto solo evita provocar la excepcion cada vez que la
     * tarea programada vuelve a pasar por un empleado ya abonado.
     */
    boolean existsByEmployeeIdAndMovementTypeAndPeriodKey(UUID employeeId, MovementType type, String periodKey);

    /**
     * Movimientos de un tipo dentro de una ventana de tiempo.
     *
     * <p>Con esto se resuelve QUE servicios cubre un pago de nomina sin inventar
     * una tabla de enlace: como cada pago liquida el saldo COMPLETO, las
     * comisiones abonadas entre el pago anterior y este son exactamente las que
     * ese pago cubre.</p>
     */
    List<EmployeeSettlementEntity> findByEmployeeIdAndMovementTypeAndSettledAtBetweenOrderBySettledAtAsc(
            UUID employeeId, MovementType type, LocalDateTime from, LocalDateTime to);

    /** El pago de nomina anterior a una fecha. Marca el inicio de la ventana. */
    Optional<EmployeeSettlementEntity> findFirstByEmployeeIdAndMovementTypeAndSettledAtLessThanOrderBySettledAtDesc(
            UUID employeeId, MovementType type, LocalDateTime before);
}
