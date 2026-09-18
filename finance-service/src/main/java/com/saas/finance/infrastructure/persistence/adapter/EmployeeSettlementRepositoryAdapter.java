package com.saas.finance.infrastructure.persistence.adapter;

import com.saas.finance.domain.model.EmployeeSettlement;
import com.saas.finance.domain.model.MovementType;
import com.saas.finance.domain.port.out.IEmployeeSettlementRepositoryPort;
import com.saas.finance.infrastructure.persistence.entity.EmployeeSettlementEntity;
import com.saas.finance.infrastructure.persistence.mapper.EmployeeSettlementPersistenceMapper;
import com.saas.finance.infrastructure.persistence.repository.JpaEmployeeSettlementRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class EmployeeSettlementRepositoryAdapter
        extends BaseJpaRepositoryAdapter<EmployeeSettlement, EmployeeSettlementEntity, UUID>
        implements IEmployeeSettlementRepositoryPort {

    private final JpaEmployeeSettlementRepository jpa;

    public EmployeeSettlementRepositoryAdapter(JpaEmployeeSettlementRepository jpa,
                                               EmployeeSettlementPersistenceMapper mapper) {
        super(jpa, mapper, "Movimiento de saldo"); this.jpa = jpa;
    }

    @Override public List<EmployeeSettlement> findByEmployeeId(UUID employeeId) {
        return getMapper().toDomainList(jpa.findByEmployeeIdOrderBySettledAtDesc(employeeId));
    }

    @Override public List<EmployeeSettlement> findByBusinessId(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessIdOrderBySettledAtDesc(businessId));
    }

    @Override public List<EmployeeSettlement> findByPayrollRunId(UUID payrollRunId) {
        return getMapper().toDomainList(jpa.findByPayrollRunIdOrderBySettledAtDesc(payrollRunId));
    }

    @Override public boolean existsPeriodMovement(UUID employeeId, MovementType type, String periodKey) {
        return jpa.existsByEmployeeIdAndMovementTypeAndPeriodKey(employeeId, type, periodKey);
    }

    @Override public List<EmployeeSettlement> findInWindow(UUID employeeId, MovementType type,
                                                           java.time.LocalDateTime from,
                                                           java.time.LocalDateTime to) {
        return getMapper().toDomainList(
                jpa.findByEmployeeIdAndMovementTypeAndSettledAtBetweenOrderBySettledAtAsc(
                        employeeId, type, from, to));
    }

    @Override public java.util.Optional<EmployeeSettlement> findPreviousPayout(
            UUID employeeId, java.time.LocalDateTime before) {
        return jpa.findFirstByEmployeeIdAndMovementTypeAndSettledAtLessThanOrderBySettledAtDesc(
                        employeeId, MovementType.PAYROLL, before)
                .map(getMapper()::toDomain);
    }
}
