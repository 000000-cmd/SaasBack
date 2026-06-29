package com.saas.business.domain.port.out;

import com.saas.business.domain.model.Offering;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IOfferingRepositoryPort extends IGenericRepositoryPort<Offering, UUID> {
    List<Offering> findByBusinessId(UUID businessId);
}
