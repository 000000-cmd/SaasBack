package com.saas.system.application.service.location;

import com.saas.common.events.EventTypes;
import com.saas.common.exception.DuplicateResourceException;
import com.saas.common.outbox.OutboxPublisher;
import com.saas.common.service.CodeCrudService;
import com.saas.system.application.dto.event.location.MunicipalityEventPayload;
import com.saas.system.domain.model.location.Country;
import com.saas.system.domain.model.location.Department;
import com.saas.system.domain.model.location.Municipality;
import com.saas.system.domain.port.out.location.ICountryRepositoryPort;
import com.saas.system.domain.port.out.location.IDepartmentRepositoryPort;
import com.saas.system.domain.port.out.location.IMunicipalityRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MunicipalityService extends CodeCrudService<Municipality, UUID> {

    private final IMunicipalityRepositoryPort muniRepo;
    private final IDepartmentRepositoryPort deptRepo;
    private final ICountryRepositoryPort countryRepo;
    private final OutboxPublisher outbox;

    public MunicipalityService(IMunicipalityRepositoryPort muniRepo,
                               IDepartmentRepositoryPort deptRepo,
                               ICountryRepositoryPort countryRepo,
                               OutboxPublisher outbox) {
        super(muniRepo);
        this.muniRepo = muniRepo;
        this.deptRepo = deptRepo;
        this.countryRepo = countryRepo;
        this.outbox = outbox;
    }

    @Override
    protected String getResourceName() {
        return "Municipio";
    }

    @Override
    protected void applyChanges(Municipality existing, Municipality incoming) {
        if (incoming.getCode() != null)         existing.setCode(incoming.getCode());
        if (incoming.getName() != null)         existing.setName(incoming.getName());
        if (incoming.getDepartmentId() != null) existing.setDepartmentId(incoming.getDepartmentId());
    }

    @Override
    protected void onBeforeCreate(Municipality entity) {
        if (entity.getDepartmentId() == null) {
            throw new IllegalArgumentException("departmentId es obligatorio");
        }
        if (muniRepo.existsByDepartmentIdAndCode(entity.getDepartmentId(), entity.getCode())) {
            throw new DuplicateResourceException(getResourceName(), "Code", entity.getCode());
        }
    }

    @Override
    protected void onBeforeUpdate(Municipality existing, Municipality incoming) {
        UUID targetDept = incoming.getDepartmentId() != null ? incoming.getDepartmentId() : existing.getDepartmentId();
        String targetCode = incoming.getCode() != null ? incoming.getCode() : existing.getCode();
        boolean deptChanged = !targetDept.equals(existing.getDepartmentId());
        boolean codeChanged = !targetCode.equals(existing.getCode());
        if ((deptChanged || codeChanged)
                && muniRepo.existsByDepartmentIdAndCode(targetDept, targetCode)) {
            throw new DuplicateResourceException(getResourceName(), "Code", targetCode);
        }
    }

    @Transactional(readOnly = true)
    public List<Municipality> findByDepartmentId(UUID departmentId) {
        return muniRepo.findByDepartmentId(departmentId);
    }

    private MunicipalityEventPayload buildPayload(Municipality m) {
        Department dept = m.getDepartmentId() != null
                ? deptRepo.findById(m.getDepartmentId()).orElse(null) : null;
        Country country = (dept != null && dept.getCountryId() != null)
                ? countryRepo.findById(dept.getCountryId()).orElse(null) : null;
        return MunicipalityEventPayload.from(m, dept, country);
    }

    @Override
    protected void onAfterCreate(Municipality saved) {
        outbox.publish(EventTypes.LOCATION_MUNICIPALITY_CREATED, null, "municipality", saved.getId(),
                buildPayload(saved));
    }

    @Override
    protected void onAfterUpdate(Municipality existing, Municipality updated) {
        outbox.publish(EventTypes.LOCATION_MUNICIPALITY_UPDATED, null, "municipality", updated.getId(),
                buildPayload(updated));
    }

    @Override
    protected void onAfterDelete(UUID id, Municipality snapshot) {
        outbox.publish(EventTypes.LOCATION_MUNICIPALITY_DELETED, null, "municipality", id,
                buildPayload(snapshot));
    }
}
