package com.saas.business.domain.port.out;

import com.saas.business.domain.model.Business;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.util.UUID;

public interface IBusinessRepositoryPort extends IGenericRepositoryPort<Business, UUID> {
}
