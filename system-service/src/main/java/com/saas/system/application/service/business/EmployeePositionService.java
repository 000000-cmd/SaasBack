package com.saas.system.application.service.business;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.business.EmployeePosition;
import com.saas.system.domain.port.out.business.IEmployeePositionRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmployeePositionService extends BaseCatalogService<EmployeePosition, UUID> {

    public EmployeePositionService(IEmployeePositionRepositoryPort repository) {
        super(repository);
    }

    @Override
    public String getCatalogPath() {
        return "employee_position";
    }

    @Override
    public EmployeePosition newInstance() {
        return new EmployeePosition();
    }

    @Override
    protected String getResourceName() {
        return "Cargo de empleado";
    }
}
