package com.saas.finance.application.service;

import com.saas.finance.domain.model.EmployeeSettlement;
import com.saas.finance.domain.model.MovementType;
import com.saas.finance.domain.model.ServiceCharge;
import com.saas.finance.domain.port.out.IEmployeeSettlementRepositoryPort;
import com.saas.finance.domain.port.out.IServiceChargeRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Que servicios cubre un pago de nomina.
 *
 * <p>Vive suelto porque lo necesitan dos: el comprobante que se consulta despues
 * y el correo que sale en el momento de dispersar. Si viviera en cualquiera de
 * los dos, el otro tendria que depender de el y se cerraria un ciclo.</p>
 *
 * <p>No hay tabla de enlace entre un pago y sus servicios, y no hace falta: cada
 * dispersion liquida el saldo COMPLETO, asi que las comisiones abonadas despues
 * del pago anterior y hasta este son exactamente las que este pago cubre. La
 * ventana se deriva de datos que ya existen en vez de duplicar la relacion en
 * una tabla que habria que mantener en sincronia.</p>
 *
 * <p>ponytail: vale mientras un pago liquide siempre el saldo entero. Si algun
 * dia se permiten pagos parciales, hara falta el enlace explicito.</p>
 */
@Service
@RequiredArgsConstructor
public class PayoutServicesResolver {

    private final IEmployeeSettlementRepositoryPort movements;
    private final IServiceChargeRepositoryPort charges;

    /** Los servicios del pago, uno por uno y SIN agrupar, en orden de fecha. */
    @Transactional(readOnly = true)
    public List<ServiceCharge> of(EmployeeSettlement payout) {
        if (payout == null || payout.getMovementType() != MovementType.PAYROLL) return List.of();

        LocalDateTime hasta = payout.getSettledAt();
        LocalDateTime desde = movements.findPreviousPayout(payout.getEmployeeId(), hasta)
                .map(EmployeeSettlement::getSettledAt)
                // Sin pago anterior, la ventana arranca en el principio de los
                // tiempos: este pago cubre todo lo que se le habia liquidado.
                .orElse(LocalDateTime.of(1970, 1, 1, 0, 0));

        List<UUID> liquidaciones = movements
                .findInWindow(payout.getEmployeeId(), MovementType.COMMISSION, desde, hasta)
                .stream()
                // Estrictamente DESPUES del pago anterior: esa liquidacion ya la
                // cobro con el pago de entonces.
                .filter(s -> s.getSettledAt().isAfter(desde))
                .map(EmployeeSettlement::getId)
                .toList();

        return charges.findBySettlements(payout.getEmployeeId(), liquidaciones);
    }
}
