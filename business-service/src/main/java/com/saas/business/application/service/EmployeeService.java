package com.saas.business.application.service;

import com.saas.business.domain.model.Employee;
import com.saas.business.domain.port.in.IEmployeeUseCase;
import com.saas.business.domain.port.out.IEmployeeRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class EmployeeService extends GenericCrudService<Employee, UUID> implements IEmployeeUseCase {

    private final IEmployeeRepositoryPort repo;
    public EmployeeService(IEmployeeRepositoryPort repo) { super(repo); this.repo = repo; }

    @Override protected String getResourceName() { return "Empleado"; }

    @Override
    protected void applyChanges(Employee existing, Employee incoming) {
        if (incoming.getBranchId() != null)        existing.setBranchId(incoming.getBranchId());
        if (incoming.getPositionId() != null)      existing.setPositionId(incoming.getPositionId());
        if (incoming.getEmployeeCode() != null)    existing.setEmployeeCode(incoming.getEmployeeCode());
        if (incoming.getHireDate() != null)        existing.setHireDate(incoming.getHireDate());
        if (incoming.getTerminationDate() != null) existing.setTerminationDate(incoming.getTerminationDate());
        if (incoming.getStatusId() != null)        existing.setStatusId(incoming.getStatusId());
    }

    @Override @Transactional(readOnly = true)
    public List<Employee> findByBranch(UUID branchId) { return repo.findByBranchId(branchId); }
    @Override @Transactional(readOnly = true)
    public List<Employee> findByThirdParty(UUID thirdPartyId) { return repo.findByThirdPartyId(thirdPartyId); }
}
