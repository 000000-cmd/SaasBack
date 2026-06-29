package com.saas.business.domain.port.in;

import com.saas.business.domain.model.BusinessOwner;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.UUID;

public interface IBusinessOwnerUseCase extends IGenericUseCase<BusinessOwner, UUID> {
    List<BusinessOwner> findByBusiness(UUID businessId);
    List<BusinessOwner> findByThirdParty(UUID thirdPartyId);
}
