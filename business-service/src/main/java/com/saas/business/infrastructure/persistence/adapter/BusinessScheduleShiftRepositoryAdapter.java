package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BusinessScheduleShift;
import com.saas.business.domain.port.out.IBusinessScheduleShiftRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BusinessScheduleShiftEntity;
import com.saas.business.infrastructure.persistence.mapper.BusinessScheduleShiftPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBusinessScheduleShiftRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class BusinessScheduleShiftRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BusinessScheduleShift, BusinessScheduleShiftEntity, UUID>
        implements IBusinessScheduleShiftRepositoryPort {
    private final JpaBusinessScheduleShiftRepository jpa;
    public BusinessScheduleShiftRepositoryAdapter(JpaBusinessScheduleShiftRepository jpa, BusinessScheduleShiftPersistenceMapper mapper) {
        super(jpa, mapper, "Turno de horario de empresa"); this.jpa = jpa;
    }
    @Override public List<BusinessScheduleShift> findByBusinessScheduleId(UUID businessScheduleId) {
        return getMapper().toDomainList(jpa.findByBusinessScheduleId(businessScheduleId));
    }
}
