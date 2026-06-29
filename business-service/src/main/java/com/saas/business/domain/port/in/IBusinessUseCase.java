package com.saas.business.domain.port.in;

import com.saas.business.domain.model.Business;
import com.saas.common.port.in.IGenericUseCase;

import java.util.UUID;

public interface IBusinessUseCase extends IGenericUseCase<Business, UUID> {
}
