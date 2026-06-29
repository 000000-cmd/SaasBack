package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.Branch;
import com.saas.business.domain.port.out.IBranchRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BranchEntity;
import com.saas.business.infrastructure.persistence.mapper.BranchPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBranchRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class BranchRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Branch, BranchEntity, UUID>
        implements IBranchRepositoryPort {
    private final JpaBranchRepository jpa;
    public BranchRepositoryAdapter(JpaBranchRepository jpa, BranchPersistenceMapper mapper) {
        super(jpa, mapper, "Sede"); this.jpa = jpa;
    }
    @Override public List<Branch> findByBusinessId(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessId(businessId));
    }
}
