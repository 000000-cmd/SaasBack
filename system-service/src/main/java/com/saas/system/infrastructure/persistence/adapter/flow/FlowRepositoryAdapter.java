package com.saas.system.infrastructure.persistence.adapter.flow;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.flow.Flow;
import com.saas.system.domain.port.out.flow.IFlowRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.flow.FlowEntity;
import com.saas.system.infrastructure.persistence.mapper.flow.FlowPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.flow.JpaFlowRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FlowRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Flow, FlowEntity, UUID>
        implements IFlowRepositoryPort {

    private final JpaFlowRepository jpa;

    public FlowRepositoryAdapter(JpaFlowRepository jpa, FlowPersistenceMapper mapper) {
        super(jpa, mapper, "Flujo");
        this.jpa = jpa;
    }

    @Override public Optional<Flow> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }
    @Override public boolean existsByCode(String code) { return jpa.existsByCode(code); }
    @Override public List<Flow> findAllOrdered() {
        return getMapper().toDomainList(jpa.findAllByOrderByCodeAsc());
    }
}
