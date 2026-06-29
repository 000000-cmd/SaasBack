package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.Employee;
import com.saas.business.domain.port.out.IEmployeeRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.EmployeeEntity;
import com.saas.business.infrastructure.persistence.mapper.EmployeePersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaEmployeeRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class EmployeeRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Employee, EmployeeEntity, UUID>
        implements IEmployeeRepositoryPort {
    private final JpaEmployeeRepository jpa;
    public EmployeeRepositoryAdapter(JpaEmployeeRepository jpa, EmployeePersistenceMapper mapper) {
        super(jpa, mapper, "Empleado"); this.jpa = jpa;
    }
    @Override public List<Employee> findByBranchId(UUID branchId) {
        return getMapper().toDomainList(jpa.findByBranchId(branchId));
    }
    @Override public List<Employee> findByThirdPartyId(UUID thirdPartyId) {
        return getMapper().toDomainList(jpa.findByThirdPartyId(thirdPartyId));
    }
}
