package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.EmployeeCompensation;
import com.saas.business.domain.port.out.IEmployeeCompensationRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.EmployeeCompensationEntity;
import com.saas.business.infrastructure.persistence.mapper.EmployeeCompensationPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaEmployeeCompensationRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class EmployeeCompensationRepositoryAdapter
        extends BaseJpaRepositoryAdapter<EmployeeCompensation, EmployeeCompensationEntity, UUID>
        implements IEmployeeCompensationRepositoryPort {
    private final JpaEmployeeCompensationRepository jpa;
    public EmployeeCompensationRepositoryAdapter(JpaEmployeeCompensationRepository jpa, EmployeeCompensationPersistenceMapper mapper) {
        super(jpa, mapper, "Compensacion de empleado"); this.jpa = jpa;
    }
    @Override public List<EmployeeCompensation> findByEmployeeId(UUID employeeId) {
        return getMapper().toDomainList(jpa.findByEmployeeId(employeeId));
    }
    @Override public List<EmployeeCompensation> findByEmployeeIdAndValidToIsNull(UUID employeeId) {
        return getMapper().toDomainList(jpa.findByEmployeeIdAndValidToIsNull(employeeId));
    }
}
