package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BusinessClient;
import com.saas.business.domain.port.out.IBusinessClientRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BusinessClientEntity;
import com.saas.business.infrastructure.persistence.mapper.BusinessClientPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBusinessClientRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BusinessClientRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BusinessClient, BusinessClientEntity, UUID>
        implements IBusinessClientRepositoryPort {

    private final JpaBusinessClientRepository jpa;

    public BusinessClientRepositoryAdapter(JpaBusinessClientRepository jpa,
                                           BusinessClientPersistenceMapper mapper) {
        super(jpa, mapper, "Cliente");
        this.jpa = jpa;
    }

    @Override
    public List<BusinessClient> findByBusinessId(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessIdOrderByDisplayNameAsc(businessId));
    }

    @Override
    public Optional<BusinessClient> findByBusinessAndPhone(UUID businessId, String phoneE164) {
        return jpa.findByBusinessAndPhone(businessId, phoneE164).map(getMapper()::toDomain);
    }

    @Override
    public Optional<BusinessClient> findByBusinessAndThirdParty(UUID businessId, UUID thirdPartyId) {
        return jpa.findByBusinessAndThirdParty(businessId, thirdPartyId).map(getMapper()::toDomain);
    }
}
