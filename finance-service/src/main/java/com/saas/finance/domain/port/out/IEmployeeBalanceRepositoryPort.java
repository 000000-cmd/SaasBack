package com.saas.finance.domain.port.out;

import com.saas.finance.domain.model.EmployeeBalance;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.Optional;
import java.util.UUID;

public interface IEmployeeBalanceRepositoryPort extends IGenericRepositoryPort<EmployeeBalance, UUID> {
    Optional<EmployeeBalance> findByEmployeeId(UUID employeeId);
    Optional<EmployeeBalance> findByUserId(UUID userId);
}
