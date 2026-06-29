package com.saas.thirdparty.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.thirdparty.domain.model.ThirdPartyContact;

import java.util.List;
import java.util.UUID;

public interface IThirdPartyContactRepositoryPort extends IGenericRepositoryPort<ThirdPartyContact, UUID> {
    List<ThirdPartyContact> findByThirdPartyId(UUID thirdPartyId);
}
