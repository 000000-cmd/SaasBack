package com.saas.finance.infrastructure.persistence.adapter;

import com.saas.finance.domain.model.EmployeeSettlement;
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
        super(jpa, mapper, "Liquidacion de empleado"); this.jpa = jpa;
    }

    @Override public List<EmployeeSettlement> findByEmployeeId(UUID employeeId) {
        return getMapper().toDomainList(jpa.findByEmployeeIdOrderBySettledAtDesc(employeeId));
    }

    @Override public List<EmployeeSettlement> findByBusinessId(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessIdOrderBySettledAtDesc(businessId));
    }
}
