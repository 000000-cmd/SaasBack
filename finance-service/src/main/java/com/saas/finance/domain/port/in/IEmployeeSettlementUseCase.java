package com.saas.finance.domain.port.in;

import com.saas.finance.domain.model.EmployeeSettlement;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface IEmployeeSettlementUseCase {

    /**
     * Confirma la liquidacion de un empleado: registra el movimiento y suma
     * {@code amount} a lo pagado de su saldo (baja el por cobrar). Irreversible.
     *
     * @param amount monto a liquidar; si es null se liquida TODO el saldo por cobrar.
     */
    EmployeeSettlement settle(UUID employeeId, BigDecimal amount, String note);

    List<EmployeeSettlement> historyByEmployee(UUID employeeId);
    List<EmployeeSettlement> historyByBusiness(UUID businessId);
}
