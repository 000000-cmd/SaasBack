package com.saas.finance.infrastructure.persistence.repository;

import com.saas.finance.infrastructure.persistence.entity.BusinessCompensationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaBusinessCompensationRepository extends JpaRepository<BusinessCompensationEntity, UUID> {
    List<BusinessCompensationEntity> findByBusinessId(UUID businessId);
    List<BusinessCompensationEntity> findByBusinessIdAndValidToIsNull(UUID businessId);
}
