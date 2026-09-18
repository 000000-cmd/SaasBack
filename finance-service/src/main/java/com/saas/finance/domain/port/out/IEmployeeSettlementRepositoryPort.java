package com.saas.finance.domain.port.out;

import com.saas.finance.domain.model.EmployeeSettlement;
import com.saas.finance.domain.model.MovementType;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEmployeeSettlementRepositoryPort extends IGenericRepositoryPort<EmployeeSettlement, UUID> {
    List<EmployeeSettlement> findByEmployeeId(UUID employeeId);
    List<EmployeeSettlement> findByBusinessId(UUID businessId);
    List<EmployeeSettlement> findByPayrollRunId(UUID payrollRunId);
    boolean existsPeriodMovement(UUID employeeId, MovementType type, String periodKey);

    /** Movimientos de un tipo en una ventana de tiempo (ambos extremos incluidos). */
    List<EmployeeSettlement> findInWindow(UUID employeeId, MovementType type,
                                          LocalDateTime from, LocalDateTime to);

    /** El pago de nomina inmediatamente anterior a {@code before}, si lo hay. */
    Optional<EmployeeSettlement> findPreviousPayout(UUID employeeId, LocalDateTime before);
}
