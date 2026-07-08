package com.saas.finance.application.service;

import com.saas.finance.domain.model.EmployeeCompensation;
import com.saas.finance.domain.port.in.IEmployeeCompensationUseCase;
import com.saas.finance.domain.port.out.IEmployeeCompensationRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmployeeCompensationService extends GenericCrudService<EmployeeCompensation, UUID>
        implements IEmployeeCompensationUseCase {
    private final IEmployeeCompensationRepositoryPort repo;
    public EmployeeCompensationService(IEmployeeCompensationRepositoryPort repo) { super(repo); this.repo = repo; }
    @Override protected String getResourceName() { return "Compensacion de empleado"; }

    @Override protected void onBeforeCreate(EmployeeCompensation e) {
        if (e.getValidFrom() == null) e.setValidFrom(LocalDateTime.now());
        e.setValidTo(null);
    }
    @Override protected void applyChanges(EmployeeCompensation e, EmployeeCompensation i) {
        if (i.getCompensationType() != null) e.setCompensationType(i.getCompensationType());
        if (i.getCompensationValue() != null) e.setCompensationValue(i.getCompensationValue());
    }

    @Override @Transactional
    public EmployeeCompensation supersede(UUID currentId, EmployeeCompensation incoming) {
        EmployeeCompensation current = getById(currentId);
        LocalDateTime now = LocalDateTime.now();
        current.setValidTo(now);
        repository.update(current);
        incoming.setEmployeeId(current.getEmployeeId());
        incoming.setValidFrom(now);
        incoming.setValidTo(null);
        return create(incoming);
    }

    @Override @Transactional(readOnly = true)
    public List<EmployeeCompensation> findByEmployee(UUID employeeId) { return repo.findByEmployeeId(employeeId); }
    @Override @Transactional(readOnly = true)
    public Optional<EmployeeCompensation> findCurrentByEmployee(UUID employeeId) {
        return repo.findByEmployeeIdAndValidToIsNull(employeeId).stream().findFirst();
    }
}
