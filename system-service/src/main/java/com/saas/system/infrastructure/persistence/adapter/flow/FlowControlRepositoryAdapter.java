package com.saas.system.infrastructure.persistence.adapter.flow;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.flow.FlowControl;
import com.saas.system.domain.port.out.flow.IFlowControlRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.flow.FlowControlEntity;
import com.saas.system.infrastructure.persistence.mapper.flow.FlowControlPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.flow.JpaFlowControlRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FlowControlRepositoryAdapter
        extends BaseJpaRepositoryAdapter<FlowControl, FlowControlEntity, UUID>
        implements IFlowControlRepositoryPort {

    private final JpaFlowControlRepository jpa;

    public FlowControlRepositoryAdapter(JpaFlowControlRepository jpa, FlowControlPersistenceMapper mapper) {
        super(jpa, mapper, "Control de flujo");
        this.jpa = jpa;
    }

    @Override public List<FlowControl> findBySections(Collection<UUID> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) return List.of();
        return getMapper().toDomainList(jpa.findBySectionIdInOrderByDisplayOrderAsc(sectionIds));
    }
    @Override public List<FlowControl> findBySection(UUID sectionId) {
        return getMapper().toDomainList(jpa.findBySectionIdOrderByDisplayOrderAsc(sectionId));
    }
}
