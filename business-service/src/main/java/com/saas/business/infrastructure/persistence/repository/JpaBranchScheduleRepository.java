package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BranchScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaBranchScheduleRepository extends JpaRepository<BranchScheduleEntity, UUID> {
    List<BranchScheduleEntity> findByBranchId(UUID branchId);
    List<BranchScheduleEntity> findByBranchIdAndValidToIsNull(UUID branchId);
}
