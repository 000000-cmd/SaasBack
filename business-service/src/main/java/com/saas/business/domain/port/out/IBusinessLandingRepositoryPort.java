package com.saas.business.domain.port.out;

import com.saas.business.domain.model.BusinessLanding;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.util.Optional;
import java.util.UUID;

public interface IBusinessLandingRepositoryPort extends IGenericRepositoryPort<BusinessLanding, UUID> {
    Optional<BusinessLanding> findByBusinessId(UUID businessId);
}
