package com.saas.business.domain.port.in;

import com.saas.business.domain.model.EmployeeOffering;
import com.saas.common.port.in.IGenericUseCase;

import java.util.List;
import java.util.UUID;

public interface IEmployeeOfferingUseCase extends IGenericUseCase<EmployeeOffering, UUID> {
    List<EmployeeOffering> findByEmployee(UUID employeeId);
    /** Reemplaza de golpe los servicios de un empleado. Es como se edita en pantalla. */
    List<EmployeeOffering> replaceForEmployee(UUID employeeId, List<EmployeeOffering> desired);
}
