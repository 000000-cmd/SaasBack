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

    @Override
    @Transactional
    public EmployeeBalance registerPayment(UUID employeeId, BigDecimal amount) {
        EmployeeBalance b = repo.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Saldo", "employeeId", employeeId));
        BigDecimal paid = b.getAmountPaid() == null ? BigDecimal.ZERO : b.getAmountPaid();
        b.setAmountPaid(paid.add(amount));
        return recompute(b);
    }

    /** Recalcula montos, persiste y publica a ES. */
    private EmployeeBalance recompute(EmployeeBalance b) {
        // TODO(finance): cuando exista el modulo de servicios prestados/pagos,
        // amountAccrued = suma(servicios del empleado x compensacion efectiva) y
        // amountPaid = suma(pagos registrados). Por ahora 0 (base lista).
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
}
