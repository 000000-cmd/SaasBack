package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaEmployeeRepository extends JpaRepository<EmployeeEntity, UUID> {
    List<EmployeeEntity> findByBranchId(UUID branchId);
    List<EmployeeEntity> findByThirdPartyId(UUID thirdPartyId);
}
