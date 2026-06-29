package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.Client;
import com.saas.business.domain.port.out.IClientRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.ClientEntity;
import com.saas.business.infrastructure.persistence.mapper.ClientPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaClientRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ClientRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Client, ClientEntity, UUID>
        implements IClientRepositoryPort {
    private final JpaClientRepository jpa;
    public ClientRepositoryAdapter(JpaClientRepository jpa, ClientPersistenceMapper mapper) {
        super(jpa, mapper, "Cliente"); this.jpa = jpa;
    }
    @Override public Optional<Client> findByThirdPartyId(UUID thirdPartyId) {
        return jpa.findByThirdPartyId(thirdPartyId).map(getMapper()::toDomain);
    }
    @Override public boolean existsByThirdPartyId(UUID thirdPartyId) {
        return jpa.existsByThirdPartyId(thirdPartyId);
    }
}
