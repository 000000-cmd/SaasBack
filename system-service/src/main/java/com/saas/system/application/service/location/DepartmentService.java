package com.saas.system.application.service.location;

import com.saas.common.events.EventTypes;
import com.saas.common.exception.DuplicateResourceException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.outbox.OutboxPublisher;
import com.saas.common.service.CodeCrudService;
import com.saas.system.application.dto.event.location.DepartmentEventPayload;
import com.saas.system.domain.model.location.Country;
import com.saas.system.domain.model.location.Department;
import com.saas.system.domain.port.out.location.ICountryRepositoryPort;
import com.saas.system.domain.port.out.location.IDepartmentRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DepartmentService extends CodeCrudService<Department, UUID> {

    private final IDepartmentRepositoryPort deptRepo;
    private final ICountryRepositoryPort countryRepo;
    private final OutboxPublisher outbox;

    public DepartmentService(IDepartmentRepositoryPort deptRepo,
                             ICountryRepositoryPort countryRepo,
                             OutboxPublisher outbox) {
        super(deptRepo);
        this.deptRepo = deptRepo;
        this.countryRepo = countryRepo;
        this.outbox = outbox;
    }

    @Override
    protected String getResourceName() {
        return "Departamento";
    }

    @Override
    protected void applyChanges(Department existing, Department incoming) {
        if (incoming.getCode() != null)      existing.setCode(incoming.getCode());
        if (incoming.getName() != null)      existing.setName(incoming.getName());
        if (incoming.getCountryId() != null) existing.setCountryId(incoming.getCountryId());
    }

    @Override
    protected void onBeforeCreate(Department entity) {
        if (entity.getCountryId() == null) {
            throw new IllegalArgumentException("countryId es obligatorio");
        }
        if (deptRepo.existsByCountryIdAndCode(entity.getCountryId(), entity.getCode())) {
            throw new DuplicateResourceException(getResourceName(), "Code", entity.getCode());
        }
    }

    @Override
    protected void onBeforeUpdate(Department existing, Department incoming) {
        UUID targetCountry = incoming.getCountryId() != null ? incoming.getCountryId() : existing.getCountryId();
        String targetCode  = incoming.getCode() != null ? incoming.getCode() : existing.getCode();
        boolean countryChanged = !targetCountry.equals(existing.getCountryId());
        boolean codeChanged    = !targetCode.equals(existing.getCode());
        if ((countryChanged || codeChanged)
                && deptRepo.existsByCountryIdAndCode(targetCountry, targetCode)) {
            throw new DuplicateResourceException(getResourceName(), "Code", targetCode);
        }
    }

    @Transactional(readOnly = true)
    public List<Department> findByCountryId(UUID countryId) {
        return deptRepo.findByCountryId(countryId);
    }

    @Override
    protected void onAfterCreate(Department saved) {
        Country country = countryRepo.findById(saved.getCountryId()).orElse(null);
        outbox.publish(EventTypes.LOCATION_DEPARTMENT_CREATED, null, "department", saved.getId(),
                DepartmentEventPayload.from(saved, country));
    }

    @Override
    protected void onAfterUpdate(Department existing, Department updated) {
        Country country = countryRepo.findById(updated.getCountryId()).orElse(null);
        outbox.publish(EventTypes.LOCATION_DEPARTMENT_UPDATED, null, "department", updated.getId(),
                DepartmentEventPayload.from(updated, country));
    }

    @Override
    protected void onAfterDelete(UUID id, Department snapshot) {
        Country country = snapshot.getCountryId() != null
                ? countryRepo.findById(snapshot.getCountryId()).orElse(null)
                : null;
        outbox.publish(EventTypes.LOCATION_DEPARTMENT_DELETED, null, "department", id,
                DepartmentEventPayload.from(snapshot, country));
    }
}
