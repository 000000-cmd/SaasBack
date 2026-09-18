package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BusinessPublicProfile;
import com.saas.business.domain.port.out.IBusinessPublicProfileRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BusinessPublicProfileEntity;
import com.saas.business.infrastructure.persistence.mapper.BusinessPublicProfilePersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBusinessPublicProfileRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class BusinessPublicProfileRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BusinessPublicProfile, BusinessPublicProfileEntity, UUID>
        implements IBusinessPublicProfileRepositoryPort {

    private final JpaBusinessPublicProfileRepository jpa;

    public BusinessPublicProfileRepositoryAdapter(
            JpaBusinessPublicProfileRepository jpa,
            BusinessPublicProfilePersistenceMapper mapper) {
        super(jpa, mapper, "Ficha pública");
        this.jpa = jpa;
    }

    @Override
    public Optional<BusinessPublicProfile> findByBusinessId(UUID businessId) {
        return jpa.findByBusinessId(businessId).map(getMapper()::toDomain);
    }

    /** {@code @Transactional} con el {@code @Modifying}, no en quien llama. */
    @Override
    @Transactional
    public boolean addReview(UUID businessId, int estrellas) {
        return jpa.addReview(businessId.toString(), estrellas) == 1;
    }
}
