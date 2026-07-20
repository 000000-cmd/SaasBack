package com.saas.finance.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.finance.domain.model.EmployeeBalance;
import com.saas.finance.domain.model.EmployeeSettlement;
import com.saas.finance.domain.port.in.IEmployeeBalanceUseCase;
import com.saas.finance.domain.port.in.IEmployeeSettlementUseCase;
import com.saas.finance.domain.port.out.IEmployeeSettlementRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Liquidacion de comisiones al empleado. Confirmar mueve dinero: registra el
 * movimiento (auditoria) y suma al pagado del saldo, bajando el por cobrar.
 * Es IRREVERSIBLE, por eso valida el monto contra el saldo real antes de tocar
 * nada y ambas escrituras van en la misma transaccion.
 *
 * <p>El desglose servicio a servicio llegara con el modulo de citas; hoy se
 * liquida contra el saldo por cobrar acumulado.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeSettlementService implements IEmployeeSettlementUseCase {

    private final IEmployeeSettlementRepositoryPort repo;
    private final IEmployeeBalanceUseCase balances;

    @Override
    @Transactional
    public EmployeeSettlement settle(UUID employeeId, BigDecimal amount, String note) {
        EmployeeBalance balance = balances.findByEmployee(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Saldo", "employeeId", employeeId));

        BigDecimal pending = balance.getBalance() == null ? BigDecimal.ZERO : balance.getBalance();
        // Sin monto explicito se liquida todo lo pendiente (es el caso del boton
        // "Liberar comision" de la pantalla, que paga el saldo completo).
        BigDecimal toSettle = amount == null ? pending : amount;

        if (toSettle.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El monto a liquidar debe ser mayor que cero");
        }
        if (toSettle.compareTo(pending) > 0) {
            throw new BusinessException("El monto a liquidar supera el saldo por cobrar del empleado");
        }

        EmployeeSettlement settlement = repo.save(EmployeeSettlement.builder()
                .businessId(balance.getBusinessId())
                .branchId(balance.getBranchId())
                .employeeId(employeeId)
                .amount(toSettle)
                .balanceBefore(pending)
                .currency(balance.getCurrency())
                .settledAt(LocalDateTime.now())
                .note(note)
                .build());

        balances.registerPayment(employeeId, toSettle);
        log.info("Liquidacion confirmada employeeId={} monto={} saldoPrevio={}", employeeId, toSettle, pending);
        return settlement;
    }

    @Override @Transactional(readOnly = true)
    public List<EmployeeSettlement> historyByEmployee(UUID employeeId) { return repo.findByEmployeeId(employeeId); }

    @Override @Transactional(readOnly = true)
    public List<EmployeeSettlement> historyByBusiness(UUID businessId) { return repo.findByBusinessId(businessId); }
}
