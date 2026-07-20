package com.saas.finance.domain.port.in;

import com.saas.finance.domain.model.EmployeeBalance;
import java.util.Optional;
import java.util.UUID;

public interface IEmployeeBalanceUseCase {

    /** Crea el saldo en 0 si no existe (o actualiza el contexto), recalcula y proyecta a ES. */
    EmployeeBalance ensure(UUID employeeId, UUID businessId, UUID branchId, UUID thirdPartyId, UUID userId);

    /** Recalcula el saldo del empleado desde sus fuentes y lo proyecta a ES. No-op si no existe la fila. */
    Optional<EmployeeBalance> recalculate(UUID employeeId);

    /** Suma {@code amount} a lo pagado (baja el por cobrar), persiste y proyecta a ES. */
    EmployeeBalance registerPayment(UUID employeeId, java.math.BigDecimal amount);

    Optional<EmployeeBalance> findByEmployee(UUID employeeId);
    Optional<EmployeeBalance> findByUser(UUID userId);
}
