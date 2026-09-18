package com.saas.finance.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.finance.domain.model.*;
import com.saas.finance.domain.port.in.IBusinessCompensationUseCase;
import com.saas.finance.domain.port.in.IEmployeeBalanceUseCase;
import com.saas.finance.domain.port.in.IPayrollUseCase;
import com.saas.finance.domain.port.out.IEmployeeSettlementRepositoryPort;
import com.saas.finance.domain.port.out.IPayrollRunRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dispersion de nomina: el EGRESO. La empresa saca de su caja el saldo a favor
 * de cada colaborador y se lo consigna.
 *
 * <p>Es la otra mitad de {@link EmployeeSettlementService}: liquidar reconoce el
 * trabajo y abona; dispersar paga. El empleado ve las dos cosas en su extracto y
 * puede distinguirlas, que es justo lo que antes no podia.</p>
 *
 * <p>El sistema NO habla con el banco: no hay id de transaccion. Lo que queda es
 * la constancia interna de la corrida y, si el dueno la anoto, la cuenta destino
 * de cada movimiento.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService implements IPayrollUseCase {

    private final IPayrollRunRepositoryPort runs;
    private final IEmployeeSettlementRepositoryPort movements;
    private final IEmployeeBalanceUseCase balances;
    private final IBusinessCompensationUseCase businessComp;
    private final FinanceNotifier notifier;

    private static final DateTimeFormatter CODE_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional
    public PayrollRun disperse(UUID businessId, UUID branchId, List<PayoutOrder> orders,
                               String note, String idempotencyKey) {
        // Lo PRIMERO, antes de validar nada: si esta corrida ya se hizo, se
        // devuelve la que hay. Un doble clic en "dispersar" pagaba dos veces, y
        // el dinero ya salio: no hay pantalla que lo arregle despues.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            PayrollRun yaHecha = runs.findByIdempotencyKey(businessId, idempotencyKey).orElse(null);
            if (yaHecha != null) {
                log.info("Nomina repetida con la misma clave: se devuelve {}", yaHecha.getCode());
                return yaHecha;
            }
        }

        if (orders == null || orders.isEmpty()) {
            throw new BusinessException("Selecciona al menos un colaborador");
        }

        // Se valida TODA la orden antes de mover un peso. Rechazar a mitad de
        // camino dejaria a unos pagados y a otros no, y el dueño sin saber
        // quienes fueron cuales.
        List<PayoutOrder> sinPrueba = orders.stream()
                .filter(o -> !o.paidInCash() && isBlank(o.paymentProofUrl()))
                .toList();
        if (!sinPrueba.isEmpty()) {
            throw new BusinessException(sinPrueba.size() == 1
                    ? "Falta el comprobante de un colaborador, o marcarlo como pago en efectivo"
                    : "Faltan " + sinPrueba.size() + " comprobantes, o marcarlos como pago en efectivo");
        }

        PayrollFrequency frequency = frequencyOf(businessId);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // La corrida se guarda ANTES que sus movimientos porque cada movimiento
        // apunta a ella (FK). Al final se le escriben los totales reales.
        PayrollRun run = runs.save(PayrollRun.builder()
                .businessId(businessId)
                .branchId(branchId)
                .code(nextCode(today))
                .periodLabel(frequency.periodLabel(today))
                .periodStart(frequency.periodStart(today))
                .periodEnd(frequency.periodEnd(today))
                .employeeCount(0)
                .totalAmount(BigDecimal.ZERO)
                .currency("COP")
                .status(PayrollRun.COMPLETED)
                .executedAt(now)
                .idempotencyKey(idempotencyKey)
                .note(note)
                .build());

        BigDecimal total = BigDecimal.ZERO;
        int paid = 0;
        List<FinanceNotifier.Payout> notices = new ArrayList<>();

        for (PayoutOrder order : orders) {
            UUID employeeId = order.employeeId();
            EmployeeBalance balance = balances.findByEmployee(employeeId).orElse(null);
            if (balance == null) {
                log.warn("Nomina {}: el empleado {} no tiene saldo; se omite", run.getCode(), employeeId);
                continue;
            }

            BigDecimal payout = balance.getBalance() == null ? BigDecimal.ZERO : balance.getBalance();
            // El saldo NUNCA queda en negativo: si no hay nada a favor, no se
            // consigna nada. Es la diferencia entre pagar y prestar.
            if (payout.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("Nomina {}: {} sin saldo a favor; se omite", run.getCode(), employeeId);
                continue;
            }

            Split split = outstanding(movements.findByEmployeeId(employeeId), payout);

            EmployeeSettlement movement = movements.save(EmployeeSettlement.builder()
                    .businessId(balance.getBusinessId())
                    .branchId(balance.getBranchId())
                    .employeeId(employeeId)
                    .amount(payout)
                    .balanceBefore(payout)
                    .currency(balance.getCurrency())
                    .settledAt(now)
                    .note(note)
                    .movementType(MovementType.PAYROLL)
                    .payrollRunId(run.getId())
                    .commissionAmount(split.commission())
                    .baseSalaryAmount(split.baseSalary())
                    .payoutAccount(order.paidInCash() ? "Efectivo" : order.payoutAccount())
                    .bankAccountId(order.paidInCash() ? null : order.bankAccountId())
                    .paymentProofUrl(order.paymentProofUrl())
                    .paymentProofHash(order.paymentProofHash())
                    .paidInCash(order.paidInCash())
                    // El acuse queda pendiente a proposito: en efectivo no hay
                    // rastro salvo que la persona diga "si, lo recibi".
                    .cashConfirmedAt(null)
                    .build());

            EmployeeBalance after = balances.registerPayment(employeeId, payout);
            total = total.add(payout);
            paid++;
            notices.add(new FinanceNotifier.Payout(movement, after));
        }

        if (paid == 0) {
            throw new BusinessException("Ninguno de los colaboradores seleccionados tiene saldo a favor");
        }

        run.setEmployeeCount(paid);
        run.setTotalAmount(total);
        // PARTIAL avisa de que alguien se quedo fuera: su saldo cambio entre que
        // se listo y se ejecuto, y esconderlo haria pensar que ya se le pago.
        run.setStatus(paid == orders.size() ? PayrollRun.COMPLETED : PayrollRun.PARTIAL);
        PayrollRun saved = runs.update(run);

        log.info("Nomina dispersada code={} empleados={}/{} total={}",
                saved.getCode(), paid, orders.size(), total);
        notifier.payrollDispersed(saved, notices);
        return saved;
    }

    /**
     * Deshace una corrida de nomina.
     *
     * <h3>Se anade, no se borra</h3>
     * <p>Por cada pago se escribe un {@link MovementType#PAYROLL_REVERSAL} que
     * apunta a el. La fila del pago sigue diciendo que ese dia salio plata,
     * porque salio; el saldo vuelve por la SUMA de los dos movimientos. Borrar
     * la fila haria desaparecer el hecho, y con el la unica forma de explicar
     * despues por que el saldo cambio.</p>
     *
     * <h3>Una vez, y solo una</h3>
     * <p>Lo garantiza el indice unico sobre {@code ReversalOfId}, no un
     * {@code if}: dos peticiones simultaneas pasarian las dos la comprobacion
     * previa y devolverian el saldo dos veces — y la segunda es dinero
     * inventado.</p>
     *
     * <h3>El efectivo ya acusado no se puede deshacer</h3>
     * <p>Si el colaborador ya firmo que recibio los billetes, el dinero esta en
     * su bolsillo. Devolverle el saldo seria pagarle dos veces, y esta pantalla
     * no puede quitarle el efectivo de la mano.</p>
     */
    @Override
    @Transactional
    public PayrollRun voidRun(UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Escribe por qué se anula: queda en el historial");
        }

        PayrollRun run = byId(id);
        if (PayrollRun.VOIDED.equals(run.getStatus())) {
            throw new BusinessException("Esta dispersión ya estaba anulada");
        }

        List<EmployeeSettlement> pagos = movements.findByPayrollRunId(id).stream()
                .filter(m -> m.getMovementType() == MovementType.PAYROLL)
                .toList();
        if (pagos.isEmpty()) {
            throw new BusinessException("Esa dispersión no tiene pagos que deshacer");
        }

        List<EmployeeSettlement> acusados = pagos.stream()
                .filter(m -> Boolean.TRUE.equals(m.getPaidInCash()) && m.getCashConfirmedAt() != null)
                .toList();
        if (!acusados.isEmpty()) {
            throw new BusinessException(acusados.size() == 1
                    ? "No se puede anular: un colaborador ya confirmó que recibió el efectivo."
                    : "No se puede anular: " + acusados.size()
                      + " colaboradores ya confirmaron que recibieron el efectivo.");
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal devuelto = BigDecimal.ZERO;

        for (EmployeeSettlement pago : pagos) {
            BigDecimal importe = pago.getAmount() == null ? BigDecimal.ZERO : pago.getAmount();
            if (importe.compareTo(BigDecimal.ZERO) <= 0) continue;

            movements.save(EmployeeSettlement.builder()
                    .businessId(pago.getBusinessId())
                    .branchId(pago.getBranchId())
                    .employeeId(pago.getEmployeeId())
                    .amount(importe)
                    .balanceBefore(importe)
                    .currency(pago.getCurrency())
                    .settledAt(now)
                    .note("Anulación de " + run.getCode() + ": " + reason)
                    .movementType(MovementType.PAYROLL_REVERSAL)
                    .payrollRunId(run.getId())
                    .reversalOfId(pago.getId())
                    .commissionAmount(pago.getCommissionAmount())
                    .baseSalaryAmount(pago.getBaseSalaryAmount())
                    .build());

            balances.registerPaymentReversal(pago.getEmployeeId(), importe);
            devuelto = devuelto.add(importe);
        }

        run.setStatus(PayrollRun.VOIDED);
        run.setVoidedAt(now);
        run.setVoidReason(reason.trim());
        PayrollRun anulada = runs.update(run);

        log.info("Nomina ANULADA code={} pagos={} devuelto={} motivo={}",
                anulada.getCode(), pagos.size(), devuelto, reason);
        return anulada;
    }

    @Override @Transactional(readOnly = true)
    public List<PayrollRun> history(UUID businessId, LocalDate from, LocalDate to, int page, int size) {
        return runs.findByBusiness(businessId, from.atStartOfDay(), to.atTime(LocalTime.MAX), page, size);
    }

    @Override @Transactional(readOnly = true)
    public long countHistory(UUID businessId, LocalDate from, LocalDate to) {
        return runs.countByBusiness(businessId, from.atStartOfDay(), to.atTime(LocalTime.MAX));
    }

    @Override @Transactional(readOnly = true)
    public PayrollRun byId(UUID id) {
        return runs.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispersion de nomina", "id", id));
    }

    /**
     * Acuse de recibo de un pago en efectivo.
     *
     * <p>Comprueba que el movimiento sea DE ESE empleado: el id del movimiento
     * viaja al movil y sin esta verificacion cualquiera podria acusar el pago de
     * otro, que es exactamente la firma que se esta pidiendo.</p>
     *
     * <p>Repetirlo no cambia nada: la primera confirmacion es la que vale.</p>
     */
    @Override
    @Transactional
    public EmployeeSettlement confirmCash(UUID movementId, UUID employeeId) {
        EmployeeSettlement m = movements.findById(movementId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento", "id", movementId));

        if (!m.getEmployeeId().equals(employeeId)) {
            throw new BusinessException("Este movimiento no es tuyo");
        }
        if (!Boolean.TRUE.equals(m.getPaidInCash())) {
            throw new BusinessException("Este pago no fue en efectivo: no hay nada que confirmar");
        }
        if (m.getCashConfirmedAt() != null) return m;

        m.setCashConfirmedAt(LocalDateTime.now());
        log.info("Efectivo confirmado por el colaborador movimiento={} empleado={}", movementId, employeeId);
        return movements.update(m);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private PayrollFrequency frequencyOf(UUID businessId) {
        return businessComp.findCurrentByBusiness(businessId)
                .map(c -> PayrollFrequency.from(c.getPayrollFrequency()))
                .orElse(PayrollFrequency.MONTHLY);
    }

    /**
     * Constancia interna de la corrida. No es un id bancario y no pretende
     * serlo: es lo que el dueno cita cuando un empleado pregunta por su pago.
     */
    private String nextCode(LocalDate day) {
        for (int i = 0; i < 5; i++) {
            String code = "DP-" + day.format(CODE_DAY) + "-"
                    + String.format("%04X", ThreadLocalRandom.current().nextInt(0x10000));
            if (!runs.existsByCode(code)) return code;
        }
        // Cinco choques seguidos con 65.536 combinaciones no pasa; si pasara, el
        // sufijo del reloj lo resuelve sin dejar la corrida sin constancia.
        return "DP-" + day.format(CODE_DAY) + "-" + System.nanoTime() % 100000;
    }

    /** Cuanto del pago es comision y cuanto sueldo base. */
    private record Split(BigDecimal commission, BigDecimal baseSalary) {}

    /**
     * Desglosa el pago mirando el extracto: lo abonado por cada concepto menos lo
     * que ya se pago de ese concepto en corridas anteriores.
     *
     * <p>Se calcula aqui y se GUARDA congelado en el movimiento porque es lo que
     * el empleado va a leer en su comprobante meses despues, cuando su
     * compensacion ya sea otra.</p>
     *
     * <p>Si por deriva de datos las dos partes no suman el pago, se paga primero
     * el sueldo base y el resto va a comision: es una regla determinista, y el
     * sueldo base es la parte que el empleado tiene comprometida.</p>
     */
    private Split outstanding(List<EmployeeSettlement> history, BigDecimal payout) {
        BigDecimal creditedBase = BigDecimal.ZERO;
        BigDecimal paidBase = BigDecimal.ZERO;
        for (EmployeeSettlement m : history) {
            if (m.getMovementType() == MovementType.BASE_SALARY) {
                creditedBase = creditedBase.add(nz(m.getAmount()));
            } else if (m.getMovementType() == MovementType.PAYROLL) {
                paidBase = paidBase.add(nz(m.getBaseSalaryAmount()));
            }
        }
        BigDecimal base = creditedBase.subtract(paidBase).max(BigDecimal.ZERO).min(payout);
        return new Split(payout.subtract(base), base);
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
