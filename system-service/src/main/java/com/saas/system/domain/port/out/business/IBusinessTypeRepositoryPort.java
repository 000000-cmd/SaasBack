package com.saas.system.domain.port.out.business;

import com.saas.common.port.out.ICatalogRepositoryPort;
import com.saas.system.domain.model.business.BusinessType;

import java.util.UUID;

public interface IBusinessTypeRepositoryPort extends ICatalogRepositoryPort<BusinessType, UUID> {
}
