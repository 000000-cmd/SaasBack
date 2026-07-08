package com.saas.finance.infrastructure.persistence.repository;

import com.saas.finance.infrastructure.persistence.entity.EmployeeCompensationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaEmployeeCompensationRepository extends JpaRepository<EmployeeCompensationEntity, UUID> {
    List<EmployeeCompensationEntity> findByEmployeeId(UUID employeeId);
    List<EmployeeCompensationEntity> findByEmployeeIdAndValidToIsNull(UUID employeeId);
}
