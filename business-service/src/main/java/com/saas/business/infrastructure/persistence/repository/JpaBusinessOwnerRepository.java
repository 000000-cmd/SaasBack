package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BusinessOwnerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaBusinessOwnerRepository extends JpaRepository<BusinessOwnerEntity, UUID> {
    List<BusinessOwnerEntity> findByBusinessId(UUID businessId);
    List<BusinessOwnerEntity> findByThirdPartyId(UUID thirdPartyId);
}
