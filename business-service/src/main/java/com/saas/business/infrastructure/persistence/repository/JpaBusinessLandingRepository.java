package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BusinessLandingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaBusinessLandingRepository extends JpaRepository<BusinessLandingEntity, UUID> {
    Optional<BusinessLandingEntity> findByBusinessId(UUID businessId);
}
