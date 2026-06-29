package com.saas.business.domain.port.in;

import com.saas.business.domain.model.Employee;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.UUID;

public interface IEmployeeUseCase extends IGenericUseCase<Employee, UUID> {
    List<Employee> findByBranch(UUID branchId);
    List<Employee> findByThirdParty(UUID thirdPartyId);
}
