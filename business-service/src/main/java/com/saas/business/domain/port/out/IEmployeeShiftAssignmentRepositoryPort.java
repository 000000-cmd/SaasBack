package com.saas.business.domain.port.out;

import com.saas.business.domain.model.EmployeeShiftAssignment;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IEmployeeShiftAssignmentRepositoryPort extends IGenericRepositoryPort<EmployeeShiftAssignment, UUID> {
    List<EmployeeShiftAssignment> findByEmployeeId(UUID employeeId);
    List<EmployeeShiftAssignment> findByEmployeeIdAndValidToIsNull(UUID employeeId);
    /** Asignaciones de varios empleados vigentes en un rango. Alimenta el motor. */
    List<EmployeeShiftAssignment> effectiveIn(Collection<UUID> employeeIds,
                                              LocalDateTime from, LocalDateTime to);
}
