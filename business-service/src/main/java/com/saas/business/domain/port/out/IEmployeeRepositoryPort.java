package com.saas.business.domain.port.out;

import com.saas.business.domain.model.Employee;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IEmployeeRepositoryPort extends IGenericRepositoryPort<Employee, UUID> {
    List<Employee> findByBranchId(UUID branchId);
    List<Employee> findByThirdPartyId(UUID thirdPartyId);
}
