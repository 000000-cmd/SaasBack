package com.saas.system.infrastructure.persistence.adapter.flow;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.flow.FlowMessage;
import com.saas.system.domain.port.out.flow.IFlowMessageRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.flow.FlowMessageEntity;
import com.saas.system.infrastructure.persistence.mapper.flow.FlowMessagePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.flow.JpaFlowMessageRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FlowMessageRepositoryAdapter
        extends BaseJpaRepositoryAdapter<FlowMessage, FlowMessageEntity, UUID>
        implements IFlowMessageRepositoryPort {

    private final JpaFlowMessageRepository jpa;

    public FlowMessageRepositoryAdapter(JpaFlowMessageRepository jpa, FlowMessagePersistenceMapper mapper) {
        super(jpa, mapper, "Texto de flujo");
        this.jpa = jpa;
    }

    @Override public Optional<FlowMessage> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }
    @Override public boolean existsByCode(String code) { return jpa.existsByCode(code); }
    @Override public List<FlowMessage> findAllOrdered() {
        return getMapper().toDomainList(jpa.findAllByOrderByCodeAsc());
    }
}
