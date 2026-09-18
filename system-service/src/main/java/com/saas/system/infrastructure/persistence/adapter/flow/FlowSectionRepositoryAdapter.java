package com.saas.system.infrastructure.persistence.adapter.flow;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.flow.FlowSection;
import com.saas.system.domain.port.out.flow.IFlowSectionRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.flow.FlowSectionEntity;
import com.saas.system.infrastructure.persistence.mapper.flow.FlowSectionPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.flow.JpaFlowSectionRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FlowSectionRepositoryAdapter
        extends BaseJpaRepositoryAdapter<FlowSection, FlowSectionEntity, UUID>
        implements IFlowSectionRepositoryPort {

    private final JpaFlowSectionRepository jpa;

    public FlowSectionRepositoryAdapter(JpaFlowSectionRepository jpa, FlowSectionPersistenceMapper mapper) {
        super(jpa, mapper, "Seccion de flujo");
        this.jpa = jpa;
    }

    @Override public List<FlowSection> findByFlow(UUID flowId) {
        return getMapper().toDomainList(jpa.findByFlowIdOrderByDisplayOrderAsc(flowId));
    }
    @Override public Optional<FlowSection> findByFlowAndCode(UUID flowId, String code) {
        return jpa.findByFlowIdAndCode(flowId, code).map(getMapper()::toDomain);
    }
}
