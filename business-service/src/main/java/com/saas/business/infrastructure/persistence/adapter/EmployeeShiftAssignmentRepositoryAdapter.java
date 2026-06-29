package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.EmployeeShiftAssignment;
import com.saas.business.domain.port.out.IEmployeeShiftAssignmentRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.EmployeeShiftAssignmentEntity;
import com.saas.business.infrastructure.persistence.mapper.EmployeeShiftAssignmentPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaEmployeeShiftAssignmentRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class EmployeeShiftAssignmentRepositoryAdapter
        extends BaseJpaRepositoryAdapter<EmployeeShiftAssignment, EmployeeShiftAssignmentEntity, UUID>
        implements IEmployeeShiftAssignmentRepositoryPort {
    private final JpaEmployeeShiftAssignmentRepository jpa;
    public EmployeeShiftAssignmentRepositoryAdapter(JpaEmployeeShiftAssignmentRepository jpa, EmployeeShiftAssignmentPersistenceMapper mapper) {
        super(jpa, mapper, "Asignacion de turno"); this.jpa = jpa;
    }
    @Override public List<EmployeeShiftAssignment> findByEmployeeId(UUID employeeId) {
        return getMapper().toDomainList(jpa.findByEmployeeId(employeeId));
    }
    @Override public List<EmployeeShiftAssignment> findByEmployeeIdAndValidToIsNull(UUID employeeId) {
        return getMapper().toDomainList(jpa.findByEmployeeIdAndValidToIsNull(employeeId));
    }
}
