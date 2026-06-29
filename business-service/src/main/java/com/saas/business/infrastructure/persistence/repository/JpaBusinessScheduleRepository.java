package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BusinessScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaBusinessScheduleRepository extends JpaRepository<BusinessScheduleEntity, UUID> {
    List<BusinessScheduleEntity> findByBusinessId(UUID businessId);
    List<BusinessScheduleEntity> findByBusinessIdAndValidToIsNull(UUID businessId);
}
