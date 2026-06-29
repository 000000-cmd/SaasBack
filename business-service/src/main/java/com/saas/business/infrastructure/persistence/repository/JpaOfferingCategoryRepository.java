package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.OfferingCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaOfferingCategoryRepository extends JpaRepository<OfferingCategoryEntity, UUID> {
    List<OfferingCategoryEntity> findByBusinessId(UUID businessId);
}
