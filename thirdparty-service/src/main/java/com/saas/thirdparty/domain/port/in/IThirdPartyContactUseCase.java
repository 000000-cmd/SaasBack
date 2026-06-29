package com.saas.thirdparty.domain.port.in;

import com.saas.common.port.in.IGenericUseCase;
import com.saas.thirdparty.domain.model.ThirdPartyContact;

import java.util.List;
import java.util.UUID;

public interface IThirdPartyContactUseCase extends IGenericUseCase<ThirdPartyContact, UUID> {
    List<ThirdPartyContact> findByThirdParty(UUID thirdPartyId);
}
