package com.saas.business.domain.port.out;

import com.saas.business.domain.model.EmployeeCompensation;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IEmployeeCompensationRepositoryPort extends IGenericRepositoryPort<EmployeeCompensation, UUID> {
    List<EmployeeCompensation> findByEmployeeId(UUID employeeId);
    List<EmployeeCompensation> findByEmployeeIdAndValidToIsNull(UUID employeeId);
}
