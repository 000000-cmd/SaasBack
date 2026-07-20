package com.saas.finance.domain.port.out;

import com.saas.finance.domain.model.EmployeeSettlement;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IEmployeeSettlementRepositoryPort extends IGenericRepositoryPort<EmployeeSettlement, UUID> {
    List<EmployeeSettlement> findByEmployeeId(UUID employeeId);
    List<EmployeeSettlement> findByBusinessId(UUID businessId);
}
