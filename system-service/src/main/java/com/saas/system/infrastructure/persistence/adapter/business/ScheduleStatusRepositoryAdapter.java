package com.saas.system.infrastructure.persistence.adapter.business;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.business.ScheduleStatus;
import com.saas.system.domain.port.out.business.IScheduleStatusRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.business.ScheduleStatusEntity;
import com.saas.system.infrastructure.persistence.mapper.business.ScheduleStatusPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.business.JpaScheduleStatusRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ScheduleStatusRepositoryAdapter
        extends BaseJpaRepositoryAdapter<ScheduleStatus, ScheduleStatusEntity, UUID>
        implements IScheduleStatusRepositoryPort {

    private final JpaScheduleStatusRepository jpa;

    public ScheduleStatusRepositoryAdapter(JpaScheduleStatusRepository jpa,
                                           ScheduleStatusPersistenceMapper mapper) {
        super(jpa, mapper, "Estado de agenda");
        this.jpa = jpa;
    }

    @Override
    public Optional<ScheduleStatus> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
