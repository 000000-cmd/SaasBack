package com.saas.system.infrastructure.persistence.adapter.flow;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.flow.FlowField;
import com.saas.system.domain.port.out.flow.IFlowFieldRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.flow.FlowFieldEntity;
import com.saas.system.infrastructure.persistence.mapper.flow.FlowFieldPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.flow.JpaFlowFieldRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FlowFieldRepositoryAdapter
        extends BaseJpaRepositoryAdapter<FlowField, FlowFieldEntity, UUID>
        implements IFlowFieldRepositoryPort {

    private final JpaFlowFieldRepository jpa;

    public FlowFieldRepositoryAdapter(JpaFlowFieldRepository jpa, FlowFieldPersistenceMapper mapper) {
        super(jpa, mapper, "Campo de flujo");
        this.jpa = jpa;
    }

    @Override public List<FlowField> findBySections(Collection<UUID> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) return List.of();
        return getMapper().toDomainList(jpa.findBySectionIdInOrderByDisplayOrderAsc(sectionIds));
    }
    @Override public List<FlowField> findBySection(UUID sectionId) {
        return getMapper().toDomainList(jpa.findBySectionIdOrderByDisplayOrderAsc(sectionId));
    }
}
