package com.saas.system.domain.port.out.flow;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.flow.FlowField;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IFlowFieldRepositoryPort extends IGenericRepositoryPort<FlowField, UUID> {
    List<FlowField> findBySections(Collection<UUID> sectionIds);
    List<FlowField> findBySection(UUID sectionId);
}
