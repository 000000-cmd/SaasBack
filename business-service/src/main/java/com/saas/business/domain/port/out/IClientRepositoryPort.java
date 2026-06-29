package com.saas.business.domain.port.out;

import com.saas.business.domain.model.Client;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.Optional;
import java.util.UUID;

public interface IClientRepositoryPort extends IGenericRepositoryPort<Client, UUID> {
    Optional<Client> findByThirdPartyId(UUID thirdPartyId);
    boolean existsByThirdPartyId(UUID thirdPartyId);
}
