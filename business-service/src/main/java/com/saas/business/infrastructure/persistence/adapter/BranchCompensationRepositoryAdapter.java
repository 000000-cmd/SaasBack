package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BranchCompensation;
import com.saas.business.domain.port.out.IBranchCompensationRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BranchCompensationEntity;
import com.saas.business.infrastructure.persistence.mapper.BranchCompensationPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBranchCompensationRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class BranchCompensationRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BranchCompensation, BranchCompensationEntity, UUID>
        implements IBranchCompensationRepositoryPort {
    private final JpaBranchCompensationRepository jpa;
    public BranchCompensationRepositoryAdapter(JpaBranchCompensationRepository jpa, BranchCompensationPersistenceMapper mapper) {
        super(jpa, mapper, "Compensacion de sede"); this.jpa = jpa;
    }
    @Override public List<BranchCompensation> findByBranchId(UUID branchId) {
        return getMapper().toDomainList(jpa.findByBranchId(branchId));
    }
    @Override public List<BranchCompensation> findByBranchIdAndValidToIsNull(UUID branchId) {
        return getMapper().toDomainList(jpa.findByBranchIdAndValidToIsNull(branchId));
    }
}
