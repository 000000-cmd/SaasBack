package com.saas.finance.infrastructure.persistence.repository;

import com.saas.finance.infrastructure.persistence.entity.EmployeeSettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaEmployeeSettlementRepository extends JpaRepository<EmployeeSettlementEntity, UUID> {
    List<EmployeeSettlementEntity> findByEmployeeIdOrderBySettledAtDesc(UUID employeeId);
    List<EmployeeSettlementEntity> findByBusinessIdOrderBySettledAtDesc(UUID businessId);
}
