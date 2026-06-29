package com.saas.thirdparty.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.thirdparty.domain.model.ThirdPartyAddress;
import com.saas.thirdparty.domain.port.out.IThirdPartyAddressRepositoryPort;
import com.saas.thirdparty.infrastructure.persistence.entity.ThirdPartyAddressEntity;
import com.saas.thirdparty.infrastructure.persistence.mapper.ThirdPartyAddressPersistenceMapper;
import com.saas.thirdparty.infrastructure.persistence.repository.JpaThirdPartyAddressRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class ThirdPartyAddressRepositoryAdapter
        extends BaseJpaRepositoryAdapter<ThirdPartyAddress, ThirdPartyAddressEntity, UUID>
        implements IThirdPartyAddressRepositoryPort {

    private final JpaThirdPartyAddressRepository jpa;

    public ThirdPartyAddressRepositoryAdapter(JpaThirdPartyAddressRepository jpa,
                                              ThirdPartyAddressPersistenceMapper mapper) {
        super(jpa, mapper, "Direccion de tercero");
        this.jpa = jpa;
    }

    @Override
    public List<ThirdPartyAddress> findByThirdPartyId(UUID thirdPartyId) {
        return getMapper().toDomainList(jpa.findByThirdPartyId(thirdPartyId));
    }
}
