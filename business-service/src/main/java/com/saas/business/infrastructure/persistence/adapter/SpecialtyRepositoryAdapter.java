package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.Specialty;
import com.saas.business.domain.port.out.ISpecialtyRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.SpecialtyEntity;
import com.saas.business.infrastructure.persistence.mapper.SpecialtyPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaSpecialtyRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class SpecialtyRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Specialty, SpecialtyEntity, UUID>
        implements ISpecialtyRepositoryPort {
    private final JpaSpecialtyRepository jpa;
    public SpecialtyRepositoryAdapter(JpaSpecialtyRepository jpa, SpecialtyPersistenceMapper mapper) {
        super(jpa, mapper, "Especialidad"); this.jpa = jpa;
    }
    @Override public List<Specialty> findByBusinessId(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessId(businessId));
    }
}
