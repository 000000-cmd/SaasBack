package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BranchSchedule;
import com.saas.business.domain.port.out.IBranchScheduleRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BranchScheduleEntity;
import com.saas.business.infrastructure.persistence.mapper.BranchSchedulePersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBranchScheduleRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class BranchScheduleRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BranchSchedule, BranchScheduleEntity, UUID>
        implements IBranchScheduleRepositoryPort {
    private final JpaBranchScheduleRepository jpa;
    public BranchScheduleRepositoryAdapter(JpaBranchScheduleRepository jpa, BranchSchedulePersistenceMapper mapper) {
        super(jpa, mapper, "Horario de sede"); this.jpa = jpa;
    }
    @Override public List<BranchSchedule> findByBranchId(UUID branchId) {
        return getMapper().toDomainList(jpa.findByBranchId(branchId));
    }
    @Override public List<BranchSchedule> findByBranchIdAndValidToIsNull(UUID branchId) {
        return getMapper().toDomainList(jpa.findByBranchIdAndValidToIsNull(branchId));
    }
}
