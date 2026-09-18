package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.EmployeeOffering;
import com.saas.business.domain.port.out.IEmployeeOfferingRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.EmployeeOfferingEntity;
import com.saas.business.infrastructure.persistence.mapper.EmployeeOfferingPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaEmployeeOfferingRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class EmployeeOfferingRepositoryAdapter
        extends BaseJpaRepositoryAdapter<EmployeeOffering, EmployeeOfferingEntity, UUID>
        implements IEmployeeOfferingRepositoryPort {

    private final JpaEmployeeOfferingRepository jpa;

    public EmployeeOfferingRepositoryAdapter(JpaEmployeeOfferingRepository jpa,
                                             EmployeeOfferingPersistenceMapper mapper) {
        super(jpa, mapper, "Servicio del empleado");
        this.jpa = jpa;
    }

    @Override public List<EmployeeOffering> findByEmployeeId(UUID employeeId) {
        return getMapper().toDomainList(jpa.findByEmployeeId(employeeId));
    }

    @Override public List<EmployeeOffering> findByOfferingIds(Collection<UUID> offeringIds) {
        if (offeringIds == null || offeringIds.isEmpty()) return List.of();
        return getMapper().toDomainList(jpa.findByOfferingIdIn(offeringIds));
    }

    @Override public boolean exists(UUID employeeId, UUID offeringId) {
        return jpa.existsByEmployeeIdAndOfferingId(employeeId, offeringId);
    }
}
