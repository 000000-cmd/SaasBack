package com.saas.system.infrastructure.persistence.adapter.business;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.business.DayOfWeek;
import com.saas.system.domain.port.out.business.IDayOfWeekRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.business.DayOfWeekEntity;
import com.saas.system.infrastructure.persistence.mapper.business.DayOfWeekPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.business.JpaDayOfWeekRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class DayOfWeekRepositoryAdapter
        extends BaseJpaRepositoryAdapter<DayOfWeek, DayOfWeekEntity, UUID>
        implements IDayOfWeekRepositoryPort {

    private final JpaDayOfWeekRepository jpa;

    public DayOfWeekRepositoryAdapter(JpaDayOfWeekRepository jpa, DayOfWeekPersistenceMapper mapper) {
        super(jpa, mapper, "Dia de la semana");
        this.jpa = jpa;
    }

    @Override public Optional<DayOfWeek> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }
    @Override public boolean existsByCode(String code) { return jpa.existsByCode(code); }
}
