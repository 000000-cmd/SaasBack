package com.saas.finance.infrastructure.persistence.repository;

import com.saas.finance.infrastructure.persistence.entity.EmployeeBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaEmployeeBalanceRepository extends JpaRepository<EmployeeBalanceEntity, UUID> {
    Optional<EmployeeBalanceEntity> findByEmployeeId(UUID employeeId);
    Optional<EmployeeBalanceEntity> findByUserId(UUID userId);
}
