package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BusinessScheduleShiftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaBusinessScheduleShiftRepository extends JpaRepository<BusinessScheduleShiftEntity, UUID> {
    List<BusinessScheduleShiftEntity> findByBusinessScheduleId(UUID businessScheduleId);
}
