package com.saas.system.application.service.location;

import com.saas.common.events.EventTypes;
import com.saas.common.exception.DuplicateResourceException;
import com.saas.common.outbox.OutboxPublisher;
import com.saas.common.service.CodeCrudService;
import com.saas.system.application.dto.event.location.NeighborhoodEventPayload;
import com.saas.system.domain.model.location.Country;
import com.saas.system.domain.model.location.Department;
import com.saas.system.domain.model.location.Municipality;
import com.saas.system.domain.model.location.Neighborhood;
import com.saas.system.domain.port.out.location.ICountryRepositoryPort;
import com.saas.system.domain.port.out.location.IDepartmentRepositoryPort;
import com.saas.system.domain.port.out.location.IMunicipalityRepositoryPort;
import com.saas.system.domain.port.out.location.INeighborhoodRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NeighborhoodService extends CodeCrudService<Neighborhood, UUID> {

    private final INeighborhoodRepositoryPort neighRepo;
    private final IMunicipalityRepositoryPort muniRepo;
    private final IDepartmentRepositoryPort deptRepo;
    private final ICountryRepositoryPort countryRepo;
    private final OutboxPublisher outbox;

    public NeighborhoodService(INeighborhoodRepositoryPort neighRepo,
                               IMunicipalityRepositoryPort muniRepo,
                               IDepartmentRepositoryPort deptRepo,
                               ICountryRepositoryPort countryRepo,
                               OutboxPublisher outbox) {
        super(neighRepo);
        this.neighRepo = neighRepo;
        this.muniRepo = muniRepo;
        this.deptRepo = deptRepo;
        this.countryRepo = countryRepo;
        this.outbox = outbox;
    }

    @Override
    protected String getResourceName() {
        return "Barrio/Vereda";
    }

    @Override
    protected void applyChanges(Neighborhood existing, Neighborhood incoming) {
        if (incoming.getCode() != null)           existing.setCode(incoming.getCode());
        if (incoming.getName() != null)           existing.setName(incoming.getName());
        if (incoming.getType() != null)           existing.setType(incoming.getType());
        if (incoming.getMunicipalityId() != null) existing.setMunicipalityId(incoming.getMunicipalityId());
    }

    @Override
    protected void onBeforeCreate(Neighborhood entity) {
        if (entity.getMunicipalityId() == null) {
            throw new IllegalArgumentException("municipalityId es obligatorio");
        }
        if (entity.getType() == null || entity.getType().isBlank()) {
            throw new IllegalArgumentException("type es obligatorio (BARRIO|VEREDA|CORREGIMIENTO|OTRO)");
        }
        if (neighRepo.existsByMunicipalityIdAndCode(entity.getMunicipalityId(), entity.getCode())) {
            throw new DuplicateResourceException(getResourceName(), "Code", entity.getCode());
        }
    }

    @Override
    protected void onBeforeUpdate(Neighborhood existing, Neighborhood incoming) {
        UUID targetMuni = incoming.getMunicipalityId() != null ? incoming.getMunicipalityId() : existing.getMunicipalityId();
        String targetCode = incoming.getCode() != null ? incoming.getCode() : existing.getCode();
        boolean muniChanged = !targetMuni.equals(existing.getMunicipalityId());
        boolean codeChanged = !targetCode.equals(existing.getCode());
        if ((muniChanged || codeChanged)
                && neighRepo.existsByMunicipalityIdAndCode(targetMuni, targetCode)) {
            throw new DuplicateResourceException(getResourceName(), "Code", targetCode);
        }
    }

    @Transactional(readOnly = true)
    public List<Neighborhood> findByMunicipalityId(UUID municipalityId) {
        return neighRepo.findByMunicipalityId(municipalityId);
    }

    @Transactional(readOnly = true)
    public List<Neighborhood> findByMunicipalityIdAndType(UUID municipalityId, String type) {
        return neighRepo.findByMunicipalityIdAndType(municipalityId, type);
    }

    private NeighborhoodEventPayload buildPayload(Neighborhood n) {
        Municipality muni = n.getMunicipalityId() != null
                ? muniRepo.findById(n.getMunicipalityId()).orElse(null) : null;
        Department dept = (muni != null && muni.getDepartmentId() != null)
                ? deptRepo.findById(muni.getDepartmentId()).orElse(null) : null;
        Country country = (dept != null && dept.getCountryId() != null)
                ? countryRepo.findById(dept.getCountryId()).orElse(null) : null;
        return NeighborhoodEventPayload.from(n, muni, dept, country);
    }

    @Override
    protected void onAfterCreate(Neighborhood saved) {
        outbox.publish(EventTypes.LOCATION_NEIGHBORHOOD_CREATED, null, "neighborhood", saved.getId(),
                buildPayload(saved));
    }

    @Override
    protected void onAfterUpdate(Neighborhood existing, Neighborhood updated) {
        outbox.publish(EventTypes.LOCATION_NEIGHBORHOOD_UPDATED, null, "neighborhood", updated.getId(),
                buildPayload(updated));
    }

    @Override
    protected void onAfterDelete(UUID id, Neighborhood snapshot) {
        outbox.publish(EventTypes.LOCATION_NEIGHBORHOOD_DELETED, null, "neighborhood", id,
                buildPayload(snapshot));
    }
}
