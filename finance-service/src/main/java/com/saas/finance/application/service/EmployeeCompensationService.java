package com.saas.finance.application.service;

import com.saas.finance.domain.model.EmployeeCompensation;
import com.saas.finance.domain.port.in.IEmployeeBalanceUseCase;
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
    private final IEmployeeBalanceUseCase balance;
    public EmployeeCompensationService(IEmployeeCompensationRepositoryPort repo, IEmployeeBalanceUseCase balance) {
        super(repo); this.repo = repo; this.balance = balance;
    }
    @Override protected String getResourceName() { return "Compensacion de empleado"; }

    @Override protected void onBeforeCreate(EmployeeCompensation e) {
        if (e.getValidFrom() == null) e.setValidFrom(LocalDateTime.now());
        e.setValidTo(null);
    }

    // La compensacion es una entrada del calculo del saldo: al cambiarla,
    // refrescar el saldo del empleado (que se reproyecta a ES). No-op si el
    // empleado aun no tiene fila de saldo.
    @Override protected void onAfterCreate(EmployeeCompensation saved) {
        if (saved.getEmployeeId() != null) balance.recalculate(saved.getEmployeeId());
    }
    @Override protected void applyChanges(EmployeeCompensation e, EmployeeCompensation i) {
        if (i.getCompensationType() != null) e.setCompensationType(i.getCompensationType());
        if (i.getCompensationValue() != null) e.setCompensationValue(i.getCompensationValue());
        e.setSalaryBase(i.getSalaryBase()); // nullable a proposito: al cambiar a un tipo no-hibrido se limpia
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
