package com.saas.finance.application.service;

import com.saas.common.events.EventTypes;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.outbox.OutboxPublisher;
import com.saas.finance.application.dto.event.EmployeeBalanceEventPayload;
import com.saas.finance.domain.model.EmployeeBalance;
import com.saas.finance.domain.port.in.IEmployeeBalanceUseCase;
import com.saas.finance.domain.port.out.IEmployeeBalanceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Saldo por cobrar del empleado (read model materializado). Cada cambio de una
 * de sus entradas recalcula la fila y publica al outbox para que search-service
 * la proyecte a Elasticsearch (el APK lee de ahi).
 *
 * <p>Hoy {@code amountAccrued} y {@code amountPaid} son 0: no existe aun el
 * modulo de servicios prestados/pagos. Cuando exista, {@link #recalculate}
 * sumara sus servicios (x compensacion efectiva) y restara sus pagos.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeBalanceService implements IEmployeeBalanceUseCase {

    private static final String AGGREGATE_TYPE = "employee_balance";
    private static final String DEFAULT_CURRENCY = "COP";

    private final IEmployeeBalanceRepositoryPort repo;
    private final OutboxPublisher outbox;

    @Override
    @Transactional
    public EmployeeBalance ensure(UUID employeeId, UUID businessId, UUID branchId, UUID thirdPartyId, UUID userId) {
        EmployeeBalance b = repo.findByEmployeeId(employeeId).orElse(null);
        if (b == null) {
            b = EmployeeBalance.builder()
                    .employeeId(employeeId).businessId(businessId).branchId(branchId)
                    .thirdPartyId(thirdPartyId).userId(userId)
                    .amountAccrued(BigDecimal.ZERO).amountPaid(BigDecimal.ZERO).balance(BigDecimal.ZERO)
                    .currency(DEFAULT_CURRENCY)
                    .build();
            b = repo.save(b);
        } else {
            // Completar contexto que pudo llegar despues (p. ej. userId del empleado).
            if (branchId != null) b.setBranchId(branchId);
            if (thirdPartyId != null) b.setThirdPartyId(thirdPartyId);
            if (userId != null) b.setUserId(userId);
            b = repo.update(b);
        }
        return recompute(b);
    }

    @Override
    @Transactional
    public Optional<EmployeeBalance> recalculate(UUID employeeId) {
        return repo.findByEmployeeId(employeeId).map(this::recompute);
    }

    /**
     * Abono al saldo (liquidacion de servicios o sueldo base del periodo): el
     * empleado GANA. Sube el devengado y por tanto su por cobrar.
     */
    @Override
    @Transactional
    public EmployeeBalance registerCredit(UUID employeeId, BigDecimal amount) {
        EmployeeBalance b = repo.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Saldo", "employeeId", employeeId));
        BigDecimal accrued = b.getAmountAccrued() == null ? BigDecimal.ZERO : b.getAmountAccrued();
        b.setAmountAccrued(accrued.add(amount));
        return recompute(b);
    }

    /**
     * Pago al empleado (dispersion de nomina): el empleado COBRA. Sube lo pagado
     * y por tanto baja su por cobrar.
     */
    @Override
    @Transactional
    public EmployeeBalance registerPayment(UUID employeeId, BigDecimal amount) {
        EmployeeBalance b = repo.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Saldo", "employeeId", employeeId));
        BigDecimal paid = b.getAmountPaid() == null ? BigDecimal.ZERO : b.getAmountPaid();
        b.setAmountPaid(paid.add(amount));
        return recompute(b);
    }

    /**
     * Deshace un pago. Lo pagado baja; el por cobrar vuelve a subir solo.
     *
     * <p>No se deja por debajo de cero: un acumulado negativo no significa nada
     * y contamina todos los informes que salgan de el. Si el importe a deshacer
     * fuera mayor que lo pagado, algo mas esta mal — se registra y se corta en
     * cero, que es el peor dato posible pero no uno imposible.</p>
     */
    @Override
    @Transactional
    public EmployeeBalance registerPaymentReversal(UUID employeeId, BigDecimal amount) {
        EmployeeBalance b = repo.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Saldo", "employeeId", employeeId));
        BigDecimal paid = b.getAmountPaid() == null ? BigDecimal.ZERO : b.getAmountPaid();
        BigDecimal nuevo = paid.subtract(amount);
        if (nuevo.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Anulacion de {} deja lo pagado del empleado {} en negativo ({}); se corta en cero",
                    amount, employeeId, nuevo);
            nuevo = BigDecimal.ZERO;
        }
        b.setAmountPaid(nuevo);
        return recompute(b);
    }

    /** Recalcula montos, persiste y publica a ES. */
    private EmployeeBalance recompute(EmployeeBalance b) {
        // Los acumulados los mueven los MOVIMIENTOS (employee_settlement), que es
        // la fuente de verdad auditable: abonos suben el devengado, la nomina sube
        // lo pagado. Aqui solo se recalcula el derivado y se publica.
        BigDecimal accrued = b.getAmountAccrued() == null ? BigDecimal.ZERO : b.getAmountAccrued();
        BigDecimal paid = b.getAmountPaid() == null ? BigDecimal.ZERO : b.getAmountPaid();
        b.setAmountAccrued(accrued);
        b.setAmountPaid(paid);
        b.setBalance(accrued.subtract(paid));
        if (b.getCurrency() == null) b.setCurrency(DEFAULT_CURRENCY);
        b.setLastCalculatedAt(LocalDateTime.now());
        EmployeeBalance saved = repo.update(b);
        outbox.publish(EventTypes.FINANCE_BALANCE_UPDATED, saved.getBusinessId(),
                AGGREGATE_TYPE, saved.getId(), EmployeeBalanceEventPayload.from(saved));
        log.debug("Saldo recalculado employeeId={} balance={}", saved.getEmployeeId(), saved.getBalance());
        return saved;
    }

    @Override @Transactional(readOnly = true)
    public Optional<EmployeeBalance> findByEmployee(UUID employeeId) { return repo.findByEmployeeId(employeeId); }

    @Override @Transactional(readOnly = true)
    public Optional<EmployeeBalance> findByUser(UUID userId) { return repo.findByUserId(userId); }

    @Override @Transactional(readOnly = true)
    public java.util.List<EmployeeBalance> findAllPaged(int page, int size) { return repo.findAllPaged(page, size); }

    @Override @Transactional(readOnly = true)
    public long count() { return repo.count(); }

    @Override @Transactional(readOnly = true)
    public Optional<EmployeeBalance> findById(UUID id) { return repo.findById(id); }
}
