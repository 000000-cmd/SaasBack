package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.Offering;
import com.saas.business.domain.port.out.IOfferingRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.OfferingEntity;
import com.saas.business.infrastructure.persistence.mapper.OfferingPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaOfferingRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class OfferingRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Offering, OfferingEntity, UUID>
        implements IOfferingRepositoryPort {
    private final JpaOfferingRepository jpa;
    public OfferingRepositoryAdapter(JpaOfferingRepository jpa, OfferingPersistenceMapper mapper) {
        super(jpa, mapper, "Oferta"); this.jpa = jpa;
    }
    @Override public List<Offering> findByBusinessId(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessId(businessId));
    }
}
