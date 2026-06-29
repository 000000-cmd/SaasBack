package com.saas.business.application.service;

import com.saas.business.domain.model.EmployeeShiftAssignment;
import com.saas.business.domain.port.in.IEmployeeShiftAssignmentUseCase;
import com.saas.business.domain.port.out.IEmployeeShiftAssignmentRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EmployeeShiftAssignmentService extends GenericCrudService<EmployeeShiftAssignment, UUID>
        implements IEmployeeShiftAssignmentUseCase {
    private final IEmployeeShiftAssignmentRepositoryPort repo;
    public EmployeeShiftAssignmentService(IEmployeeShiftAssignmentRepositoryPort repo) { super(repo); this.repo = repo; }
    @Override protected String getResourceName() { return "Asignacion de turno"; }

    @Override protected void onBeforeCreate(EmployeeShiftAssignment e) {
        if (e.getValidFrom() == null) e.setValidFrom(LocalDateTime.now());
        e.setValidTo(null);
    }
    @Override protected void applyChanges(EmployeeShiftAssignment e, EmployeeShiftAssignment i) {
        if (i.getBranchScheduleShiftId() != null) e.setBranchScheduleShiftId(i.getBranchScheduleShiftId());
        if (i.getIsFullShift() != null) e.setIsFullShift(i.getIsFullShift());
        if (i.getCustomStartTime() != null) e.setCustomStartTime(i.getCustomStartTime());
        if (i.getCustomEndTime() != null) e.setCustomEndTime(i.getCustomEndTime());
        if (i.getStatusId() != null) e.setStatusId(i.getStatusId());
    }

    @Override @Transactional
    public EmployeeShiftAssignment supersede(UUID currentId, EmployeeShiftAssignment incoming) {
        EmployeeShiftAssignment current = getById(currentId);
        LocalDateTime now = LocalDateTime.now();
        current.setValidTo(now);
        repository.update(current);
        incoming.setEmployeeId(current.getEmployeeId());
        incoming.setValidFrom(now);
        incoming.setValidTo(null);
        return create(incoming);
    }

    @Override @Transactional(readOnly = true)
    public List<EmployeeShiftAssignment> findByEmployee(UUID employeeId) { return repo.findByEmployeeId(employeeId); }
    @Override @Transactional(readOnly = true)
    public List<EmployeeShiftAssignment> findCurrentByEmployee(UUID employeeId) { return repo.findByEmployeeIdAndValidToIsNull(employeeId); }
}
