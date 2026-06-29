package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BusinessSchedule;
import com.saas.business.domain.port.out.IBusinessScheduleRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BusinessScheduleEntity;
import com.saas.business.infrastructure.persistence.mapper.BusinessSchedulePersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBusinessScheduleRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class BusinessScheduleRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BusinessSchedule, BusinessScheduleEntity, UUID>
        implements IBusinessScheduleRepositoryPort {
    private final JpaBusinessScheduleRepository jpa;
    public BusinessScheduleRepositoryAdapter(JpaBusinessScheduleRepository jpa, BusinessSchedulePersistenceMapper mapper) {
        super(jpa, mapper, "Horario de empresa"); this.jpa = jpa;
    }
    @Override public List<BusinessSchedule> findByBusinessId(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessId(businessId));
    }
    @Override public List<BusinessSchedule> findByBusinessIdAndValidToIsNull(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessIdAndValidToIsNull(businessId));
    }
}
