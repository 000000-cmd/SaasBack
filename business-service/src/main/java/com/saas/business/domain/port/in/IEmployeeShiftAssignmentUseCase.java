package com.saas.business.domain.port.in;

import com.saas.business.domain.model.EmployeeShiftAssignment;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.UUID;

public interface IEmployeeShiftAssignmentUseCase extends IGenericUseCase<EmployeeShiftAssignment, UUID> {
    List<EmployeeShiftAssignment> findByEmployee(UUID employeeId);
    List<EmployeeShiftAssignment> findCurrentByEmployee(UUID employeeId);
    EmployeeShiftAssignment supersede(UUID currentId, EmployeeShiftAssignment incoming);
}
