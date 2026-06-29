package com.saas.system.domain.port.out.business;

import com.saas.common.port.out.ICatalogRepositoryPort;
import com.saas.system.domain.model.business.EmployeePosition;

import java.util.UUID;

public interface IEmployeePositionRepositoryPort extends ICatalogRepositoryPort<EmployeePosition, UUID> {
}
