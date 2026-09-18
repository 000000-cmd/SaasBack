package com.saas.finance.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.finance.domain.model.*;
import com.saas.finance.domain.port.in.IEmployeeBalanceUseCase;
import com.saas.finance.domain.port.out.IEmployeeSettlementRepositoryPort;
import com.saas.finance.domain.port.out.IPayrollRunRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * El papeleo de un pago ya hecho: su detalle, su extracto y su reenvio.
 *
 * <p>Existe aparte de {@link PayrollService} porque son cosas distintas: aquel
 * mueve dinero, este solo vuelve a leer y a imprimir algo que ya paso.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollDocumentService {

    private final IEmployeeSettlementRepositoryPort movements;
    private final IPayrollRunRepositoryPort runs;
    private final IEmployeeBalanceUseCase balances;
    private final PayoutServicesResolver payoutServices;
    private final FinanceNotifier notifier;

    /**
     * Todo lo que hace falta para pintar un comprobante.
     *
     * @param services los servicios que la comision de ese pago esta cubriendo,
     *        uno por uno y SIN agrupar. Es la respuesta a "de donde sale esta
     *        cifra", que es lo primero que pregunta quien recibe la factura.
     */
    public record StatementData(EmployeeSettlement movement, PayrollRun run,
                                EmployeeBalance balance, List<ServiceCharge> services) {}

    @Transactional(readOnly = true)
    public StatementData detail(UUID movementId) {
        EmployeeSettlement m = movements.findById(movementId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento", "id", movementId));

        // Solo los pagos tienen comprobante. Un abono todavia no es plata que
        // haya salido, y darle papel de pago seria justo la confusion que este
        // modulo separa.
        if (m.getMovementType() != MovementType.PAYROLL || m.getPayrollRunId() == null) {
            throw new BusinessException("Este movimiento no es un pago de nómina");
        }

        PayrollRun run = runs.findById(m.getPayrollRunId())
                .orElseThrow(() -> new ResourceNotFoundException("Corrida", "id", m.getPayrollRunId()));
        EmployeeBalance balance = balances.findByEmployee(m.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Saldo", "employeeId", m.getEmployeeId()));

        return new StatementData(m, run, balance, payoutServices.of(m));
    }

    /** El extracto en PDF, cifrado con el documento del empleado. */
    @Transactional(readOnly = true)
    public byte[] statement(UUID movementId) {
        StatementData d = detail(movementId);
        return notifier.statementOf(d.run(), d.movement(), d.balance(), d.services());
    }

    /**
     * Vuelve a mandarle el comprobante por correo al colaborador.
     *
     * <p>NO va readOnly aunque solo lea: el aviso se publica al outbox, que es un
     * INSERT, y en una transaccion de solo lectura Hibernate no vacia la sesion —
     * la fila se pierde SIN ERROR y el correo nunca sale.</p>
     */
    @Transactional
    public void resend(UUID movementId) {
        StatementData d = detail(movementId);
        notifier.resendPayrollNotice(d.run(), d.movement(), d.balance());
        log.info("Comprobante reenviado movimiento={}", movementId);
    }
}
