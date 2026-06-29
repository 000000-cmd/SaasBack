package com.saas.thirdparty.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.thirdparty.domain.model.ThirdPartyContact;
import com.saas.thirdparty.domain.port.out.IThirdPartyContactRepositoryPort;
import com.saas.thirdparty.infrastructure.persistence.entity.ThirdPartyContactEntity;
import com.saas.thirdparty.infrastructure.persistence.mapper.ThirdPartyContactPersistenceMapper;
import com.saas.thirdparty.infrastructure.persistence.repository.JpaThirdPartyContactRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class ThirdPartyContactRepositoryAdapter
        extends BaseJpaRepositoryAdapter<ThirdPartyContact, ThirdPartyContactEntity, UUID>
        implements IThirdPartyContactRepositoryPort {

    private final JpaThirdPartyContactRepository jpa;

    public ThirdPartyContactRepositoryAdapter(JpaThirdPartyContactRepository jpa,
                                              ThirdPartyContactPersistenceMapper mapper) {
        super(jpa, mapper, "Contacto de tercero");
        this.jpa = jpa;
    }

    @Override
    public List<ThirdPartyContact> findByThirdPartyId(UUID thirdPartyId) {
        return getMapper().toDomainList(jpa.findByThirdPartyId(thirdPartyId));
    }
}
