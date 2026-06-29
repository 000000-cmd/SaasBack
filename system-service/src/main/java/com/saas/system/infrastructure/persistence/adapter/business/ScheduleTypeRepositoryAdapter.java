package com.saas.system.infrastructure.persistence.adapter.business;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.business.ScheduleType;
import com.saas.system.domain.port.out.business.IScheduleTypeRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.business.ScheduleTypeEntity;
import com.saas.system.infrastructure.persistence.mapper.business.ScheduleTypePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.business.JpaScheduleTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ScheduleTypeRepositoryAdapter
        extends BaseJpaRepositoryAdapter<ScheduleType, ScheduleTypeEntity, UUID>
        implements IScheduleTypeRepositoryPort {

    private final JpaScheduleTypeRepository jpa;

    public ScheduleTypeRepositoryAdapter(JpaScheduleTypeRepository jpa, ScheduleTypePersistenceMapper mapper) {
        super(jpa, mapper, "Tipo de horario");
        this.jpa = jpa;
    }

    @Override
    public Optional<ScheduleType> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
