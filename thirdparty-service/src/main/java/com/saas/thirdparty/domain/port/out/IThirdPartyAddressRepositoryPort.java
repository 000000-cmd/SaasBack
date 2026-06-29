package com.saas.thirdparty.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.thirdparty.domain.model.ThirdPartyAddress;

import java.util.List;
import java.util.UUID;

public interface IThirdPartyAddressRepositoryPort extends IGenericRepositoryPort<ThirdPartyAddress, UUID> {
    List<ThirdPartyAddress> findByThirdPartyId(UUID thirdPartyId);
}
