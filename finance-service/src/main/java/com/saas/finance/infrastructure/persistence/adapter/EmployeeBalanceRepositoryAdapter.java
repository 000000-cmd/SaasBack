package com.saas.finance.infrastructure.persistence.adapter;

import com.saas.finance.domain.model.EmployeeBalance;
import com.saas.finance.domain.port.out.IEmployeeBalanceRepositoryPort;
import com.saas.finance.infrastructure.persistence.entity.EmployeeBalanceEntity;
import com.saas.finance.infrastructure.persistence.mapper.EmployeeBalancePersistenceMapper;
import com.saas.finance.infrastructure.persistence.repository.JpaEmployeeBalanceRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmployeeBalanceRepositoryAdapter
        extends BaseJpaRepositoryAdapter<EmployeeBalance, EmployeeBalanceEntity, UUID>
        implements IEmployeeBalanceRepositoryPort {
    private final JpaEmployeeBalanceRepository jpa;
    public EmployeeBalanceRepositoryAdapter(JpaEmployeeBalanceRepository jpa, EmployeeBalancePersistenceMapper mapper) {
        super(jpa, mapper, "Saldo de empleado"); this.jpa = jpa;
    }
    @Override public Optional<EmployeeBalance> findByEmployeeId(UUID employeeId) {
        return jpa.findByEmployeeId(employeeId).map(getMapper()::toDomain);
    }
    @Override public Optional<EmployeeBalance> findByUserId(UUID userId) {
        return jpa.findByUserId(userId).map(getMapper()::toDomain);
    }
}
