package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BranchCompensationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaBranchCompensationRepository extends JpaRepository<BranchCompensationEntity, UUID> {
    List<BranchCompensationEntity> findByBranchId(UUID branchId);
    List<BranchCompensationEntity> findByBranchIdAndValidToIsNull(UUID branchId);
}
