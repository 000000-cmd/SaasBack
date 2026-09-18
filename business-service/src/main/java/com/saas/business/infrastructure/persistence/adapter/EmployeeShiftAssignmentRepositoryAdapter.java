package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.EmployeeShiftAssignment;
import com.saas.business.domain.port.out.IEmployeeShiftAssignmentRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.EmployeeShiftAssignmentEntity;
import com.saas.business.infrastructure.persistence.mapper.EmployeeShiftAssignmentPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaEmployeeShiftAssignmentRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Collection;
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
    @Override public List<EmployeeShiftAssignment> effectiveIn(Collection<UUID> employeeIds,
                                                               LocalDateTime from, LocalDateTime to) {
        if (employeeIds == null || employeeIds.isEmpty()) return List.of();
        return getMapper().toDomainList(jpa.effectiveIn(employeeIds, from, to));
    }
}
