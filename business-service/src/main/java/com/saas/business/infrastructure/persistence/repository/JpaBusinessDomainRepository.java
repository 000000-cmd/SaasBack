package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BusinessDomainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaBusinessDomainRepository extends JpaRepository<BusinessDomainEntity, UUID> {
    Optional<BusinessDomainEntity> findBySlug(String slug);
    List<BusinessDomainEntity> findByBusinessId(UUID businessId);
}
