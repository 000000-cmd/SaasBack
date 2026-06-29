package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.OfferingCategory;
import com.saas.business.domain.port.out.IOfferingCategoryRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.OfferingCategoryEntity;
import com.saas.business.infrastructure.persistence.mapper.OfferingCategoryPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaOfferingCategoryRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class OfferingCategoryRepositoryAdapter
        extends BaseJpaRepositoryAdapter<OfferingCategory, OfferingCategoryEntity, UUID>
        implements IOfferingCategoryRepositoryPort {
    private final JpaOfferingCategoryRepository jpa;
    public OfferingCategoryRepositoryAdapter(JpaOfferingCategoryRepository jpa, OfferingCategoryPersistenceMapper mapper) {
        super(jpa, mapper, "Categoria de oferta"); this.jpa = jpa;
    }
    @Override public List<OfferingCategory> findByBusinessId(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessId(businessId));
    }
}
