package com.saas.system.domain.port.out.business;

import com.saas.common.port.out.ICatalogRepositoryPort;
import com.saas.system.domain.model.business.Status;

import java.util.UUID;

public interface IStatusRepositoryPort extends ICatalogRepositoryPort<Status, UUID> {
}
