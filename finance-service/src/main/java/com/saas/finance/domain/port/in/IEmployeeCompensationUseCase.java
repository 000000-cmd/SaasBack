package com.saas.finance.domain.port.in;

import com.saas.finance.domain.model.EmployeeCompensation;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEmployeeCompensationUseCase extends IGenericUseCase<EmployeeCompensation, UUID> {
    List<EmployeeCompensation> findByEmployee(UUID employeeId);
    Optional<EmployeeCompensation> findCurrentByEmployee(UUID employeeId);
    EmployeeCompensation supersede(UUID currentId, EmployeeCompensation incoming);
}
