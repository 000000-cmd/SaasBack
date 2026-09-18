package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BranchScheduleShiftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaBranchScheduleShiftRepository extends JpaRepository<BranchScheduleShiftEntity, UUID> {
    List<BranchScheduleShiftEntity> findByBranchScheduleId(UUID branchScheduleId);

    /** Los turnos de varios horarios de una vez. Lo usa el calculo de disponibilidad. */
    List<BranchScheduleShiftEntity> findByBranchScheduleIdIn(Collection<UUID> branchScheduleIds);
}
