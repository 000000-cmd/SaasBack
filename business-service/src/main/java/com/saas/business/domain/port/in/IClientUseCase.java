package com.saas.business.domain.port.in;

import com.saas.business.domain.model.Client;
import com.saas.common.port.in.IGenericUseCase;
import java.util.Optional;
import java.util.UUID;

public interface IClientUseCase extends IGenericUseCase<Client, UUID> {
    Optional<Client> findByThirdParty(UUID thirdPartyId);
}
