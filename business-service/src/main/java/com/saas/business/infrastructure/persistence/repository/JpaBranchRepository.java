package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaBranchRepository extends JpaRepository<BranchEntity, UUID> {
    List<BranchEntity> findByBusinessId(UUID businessId);
}
