package com.saas.system.infrastructure.persistence.adapter.business;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.business.ShiftType;
import com.saas.system.domain.port.out.business.IShiftTypeRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.business.ShiftTypeEntity;
import com.saas.system.infrastructure.persistence.mapper.business.ShiftTypePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.business.JpaShiftTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ShiftTypeRepositoryAdapter
        extends BaseJpaRepositoryAdapter<ShiftType, ShiftTypeEntity, UUID>
        implements IShiftTypeRepositoryPort {

    private final JpaShiftTypeRepository jpa;

    public ShiftTypeRepositoryAdapter(JpaShiftTypeRepository jpa, ShiftTypePersistenceMapper mapper) {
        super(jpa, mapper, "Tipo de turno");
        this.jpa = jpa;
    }

    @Override
    public Optional<ShiftType> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
