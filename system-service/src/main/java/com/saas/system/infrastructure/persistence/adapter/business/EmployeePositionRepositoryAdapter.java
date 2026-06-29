package com.saas.system.infrastructure.persistence.adapter.business;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.business.EmployeePosition;
import com.saas.system.domain.port.out.business.IEmployeePositionRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.business.EmployeePositionEntity;
import com.saas.system.infrastructure.persistence.mapper.business.EmployeePositionPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.business.JpaEmployeePositionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class EmployeePositionRepositoryAdapter
        extends BaseJpaRepositoryAdapter<EmployeePosition, EmployeePositionEntity, UUID>
        implements IEmployeePositionRepositoryPort {

    private final JpaEmployeePositionRepository jpa;

    public EmployeePositionRepositoryAdapter(JpaEmployeePositionRepository jpa, EmployeePositionPersistenceMapper mapper) {
        super(jpa, mapper, "Cargo de empleado");
        this.jpa = jpa;
    }

    @Override
    public Optional<EmployeePosition> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
