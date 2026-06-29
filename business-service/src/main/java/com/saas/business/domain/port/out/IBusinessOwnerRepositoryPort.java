package com.saas.business.domain.port.out;

import com.saas.business.domain.model.BusinessOwner;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IBusinessOwnerRepositoryPort extends IGenericRepositoryPort<BusinessOwner, UUID> {
    List<BusinessOwner> findByBusinessId(UUID businessId);
    List<BusinessOwner> findByThirdPartyId(UUID thirdPartyId);
}
