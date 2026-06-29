package com.saas.thirdparty.domain.port.in;

import com.saas.common.port.in.IGenericUseCase;
import com.saas.thirdparty.domain.model.ThirdPartyAddress;

import java.util.List;
import java.util.UUID;

public interface IThirdPartyAddressUseCase extends IGenericUseCase<ThirdPartyAddress, UUID> {
    List<ThirdPartyAddress> findByThirdParty(UUID thirdPartyId);
}
