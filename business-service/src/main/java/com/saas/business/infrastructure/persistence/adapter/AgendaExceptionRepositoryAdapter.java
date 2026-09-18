package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.AgendaException;
import com.saas.business.domain.port.out.IAgendaExceptionRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.AgendaExceptionEntity;
import com.saas.business.infrastructure.persistence.mapper.AgendaExceptionPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaAgendaExceptionRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class AgendaExceptionRepositoryAdapter
        extends BaseJpaRepositoryAdapter<AgendaException, AgendaExceptionEntity, UUID>
        implements IAgendaExceptionRepositoryPort {

    private final JpaAgendaExceptionRepository jpa;

    public AgendaExceptionRepositoryAdapter(JpaAgendaExceptionRepository jpa,
                                            AgendaExceptionPersistenceMapper mapper) {
        super(jpa, mapper, "Excepción de agenda");
        this.jpa = jpa;
    }

    @Override public List<AgendaException> overlapping(UUID businessId, Instant start, Instant end) {
        return getMapper().toDomainList(jpa.overlapping(businessId, start, end));
    }
}
