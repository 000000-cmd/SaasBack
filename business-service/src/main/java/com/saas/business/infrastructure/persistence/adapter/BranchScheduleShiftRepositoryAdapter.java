package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BranchScheduleShift;
import com.saas.business.domain.port.out.IBranchScheduleShiftRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BranchScheduleShiftEntity;
import com.saas.business.infrastructure.persistence.mapper.BranchScheduleShiftPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBranchScheduleShiftRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class BranchScheduleShiftRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BranchScheduleShift, BranchScheduleShiftEntity, UUID>
        implements IBranchScheduleShiftRepositoryPort {
    private final JpaBranchScheduleShiftRepository jpa;
    public BranchScheduleShiftRepositoryAdapter(JpaBranchScheduleShiftRepository jpa, BranchScheduleShiftPersistenceMapper mapper) {
        super(jpa, mapper, "Turno de horario de sede"); this.jpa = jpa;
    }
    @Override public List<BranchScheduleShift> findByBranchScheduleId(UUID branchScheduleId) {
        return getMapper().toDomainList(jpa.findByBranchScheduleId(branchScheduleId));
    }
}
