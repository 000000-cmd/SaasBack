package com.saas.business.domain.port.out;

import com.saas.business.domain.model.EmployeeOffering;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IEmployeeOfferingRepositoryPort extends IGenericRepositoryPort<EmployeeOffering, UUID> {
    List<EmployeeOffering> findByEmployeeId(UUID employeeId);
    List<EmployeeOffering> findByOfferingIds(Collection<UUID> offeringIds);
    boolean exists(UUID employeeId, UUID offeringId);
}
