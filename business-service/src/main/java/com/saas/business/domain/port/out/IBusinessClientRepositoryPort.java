package com.saas.business.domain.port.out;

import com.saas.business.domain.model.BusinessClient;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IBusinessClientRepositoryPort extends IGenericRepositoryPort<BusinessClient, UUID> {

    List<BusinessClient> findByBusinessId(UUID businessId);

    /** El telefono ya viene normalizado a E.164 cuando llega aqui. */
    Optional<BusinessClient> findByBusinessAndPhone(UUID businessId, String phoneE164);

    Optional<BusinessClient> findByBusinessAndThirdParty(UUID businessId, UUID thirdPartyId);
}
