package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.EmployeeShiftAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaEmployeeShiftAssignmentRepository extends JpaRepository<EmployeeShiftAssignmentEntity, UUID> {
    List<EmployeeShiftAssignmentEntity> findByEmployeeId(UUID employeeId);
    List<EmployeeShiftAssignmentEntity> findByEmployeeIdAndValidToIsNull(UUID employeeId);
}
