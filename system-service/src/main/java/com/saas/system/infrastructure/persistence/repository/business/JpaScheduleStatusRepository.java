package com.saas.system.infrastructure.persistence.repository.business;

import com.saas.system.infrastructure.persistence.entity.business.ScheduleStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaScheduleStatusRepository extends JpaRepository<ScheduleStatusEntity, UUID> {

    Optional<ScheduleStatusEntity> findByCode(String code);
    boolean existsByCode(String code);

}
