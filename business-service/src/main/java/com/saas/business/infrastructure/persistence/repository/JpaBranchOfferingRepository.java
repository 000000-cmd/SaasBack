package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BranchOfferingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaBranchOfferingRepository extends JpaRepository<BranchOfferingEntity, UUID> {
    List<BranchOfferingEntity> findByBranchId(UUID branchId);
}
