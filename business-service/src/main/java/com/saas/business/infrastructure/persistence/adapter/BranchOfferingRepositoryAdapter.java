package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BranchOffering;
import com.saas.business.domain.port.out.IBranchOfferingRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BranchOfferingEntity;
import com.saas.business.infrastructure.persistence.mapper.BranchOfferingPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBranchOfferingRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class BranchOfferingRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BranchOffering, BranchOfferingEntity, UUID>
        implements IBranchOfferingRepositoryPort {
    private final JpaBranchOfferingRepository jpa;
    public BranchOfferingRepositoryAdapter(JpaBranchOfferingRepository jpa, BranchOfferingPersistenceMapper mapper) {
        super(jpa, mapper, "Oferta de sede"); this.jpa = jpa;
    }
    @Override public List<BranchOffering> findByBranchId(UUID branchId) {
        return getMapper().toDomainList(jpa.findByBranchId(branchId));
    }
}
