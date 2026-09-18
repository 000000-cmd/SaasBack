package com.saas.finance.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.finance.domain.model.EmployeeBalance;
import com.saas.finance.domain.model.EmployeeSettlement;
import com.saas.finance.domain.model.MovementType;
import com.saas.finance.domain.model.ServiceCharge;
import com.saas.finance.domain.port.in.IEmployeeBalanceUseCase;
import com.saas.finance.domain.port.in.IEmployeeSettlementUseCase;
import com.saas.finance.domain.port.out.IEmployeeSettlementRepositoryPort;
import com.saas.finance.domain.port.out.IServiceChargeRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Liquidacion de servicios: el ABONO al saldo del empleado.
 *
 * <p>Liquidar NO es pagar. Liquidar reconoce el trabajo aprobado y lo suma al
 * saldo a favor del colaborador; consignarle esa plata es la dispersion de
 * nomina ({@link PayrollService}). Mezclar las dos era lo que hacia que
 * "liquidar" pareciera que ya se habia pagado.</p>
 *
 * <p>El monto es la SUMA DE LO APROBADO servicio a servicio, y cada cargo queda
 * sellado con el id de la liquidacion que lo pago: sin eso, "por que me pagaste
 * esto" no tiene respuesta en tres meses.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeSettlementService implements IEmployeeSettlementUseCase {

    private final IEmployeeSettlementRepositoryPort repo;
    private final IServiceChargeRepositoryPort charges;
    private final IEmployeeBalanceUseCase balances;
    private final FinanceNotifier notifier;

    @Override
    @Transactional
    public EmployeeSettlement settle(UUID employeeId, String note) {
        EmployeeBalance balance = balances.findByEmployee(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Saldo", "employeeId", employeeId));

        List<ServiceCharge> settlable = charges.findSettlable(employeeId);
        if (settlable.isEmpty()) {
            throw new BusinessException("No hay servicios aprobados pendientes de liquidar");
        }

        BigDecimal total = settlable.stream()
                .map(c -> c.getNetAmount() == null ? BigDecimal.ZERO : c.getNetAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("El total aprobado debe ser mayor que cero");
        }

        BigDecimal pending = balance.getBalance() == null ? BigDecimal.ZERO : balance.getBalance();

        EmployeeSettlement settlement = repo.save(EmployeeSettlement.builder()
                .businessId(balance.getBusinessId())
                .branchId(balance.getBranchId())
                .employeeId(employeeId)
                .amount(total)
                .balanceBefore(pending)
                .currency(balance.getCurrency())
                .settledAt(LocalDateTime.now())
                .note(note)
                .movementType(MovementType.COMMISSION)
                .commissionAmount(total)
                .baseSalaryAmount(BigDecimal.ZERO)
                .build());

        // Sellar los cargos ANTES de mover el saldo: si algo falla despues, la
        // transaccion revierte ambos y no queda un abono sin respaldo.
        for (ServiceCharge c : settlable) {
            c.setSettlementId(settlement.getId());
            charges.update(c);
        }

        EmployeeBalance after = balances.registerCredit(employeeId, total);
        log.info("Liquidacion abonada employeeId={} servicios={} monto={}", employeeId, settlable.size(), total);

        notifier.settlementConfirmed(settlement, settlable.size(), after);
        return settlement;
    }

    @Override @Transactional(readOnly = true)
    public List<EmployeeSettlement> historyByEmployee(UUID employeeId) { return repo.findByEmployeeId(employeeId); }

    @Override @Transactional(readOnly = true)
    public List<EmployeeSettlement> historyByBusiness(UUID businessId) { return repo.findByBusinessId(businessId); }

    @Override @Transactional(readOnly = true)
    public List<EmployeeSettlement> byPayrollRun(UUID payrollRunId) { return repo.findByPayrollRunId(payrollRunId); }

    @Override @Transactional(readOnly = true)
    public EmployeeSettlement byId(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento", "id", id));
    }
}
