package com.saas.finance.domain.port.in;

import com.saas.finance.domain.model.EmployeeSettlement;
import java.util.List;
import java.util.UUID;

public interface IEmployeeSettlementUseCase {

    /**
     * Liquida a un empleado: toma sus servicios APROBADOS y todavia sin liquidar,
     * los sella con la liquidacion y ABONA el total a su saldo.
     *
     * <p>Ya no recibe un monto. Ese era justo el problema: aprobar una cifra
     * suelta no dice que se esta pagando. El monto es ahora la suma de lo
     * aprobado, y si no hay nada aprobado no hay nada que liquidar.</p>
     */
    EmployeeSettlement settle(UUID employeeId, String note);

    /** Extracto del empleado: abonos y pagos, de lo mas reciente a lo mas viejo. */
    List<EmployeeSettlement> historyByEmployee(UUID employeeId);

    List<EmployeeSettlement> historyByBusiness(UUID businessId);

    /** Los movimientos de una corrida de nomina (el detalle de la dispersion). */
    List<EmployeeSettlement> byPayrollRun(UUID payrollRunId);

    /** Un movimiento suelto. Lo abre el comprobante desde su propia ruta. */
    EmployeeSettlement byId(UUID id);
}
