package com.saas.system.domain.port.out.flow;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.flow.FlowControl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IFlowControlRepositoryPort extends IGenericRepositoryPort<FlowControl, UUID> {
    List<FlowControl> findBySections(Collection<UUID> sectionIds);
    List<FlowControl> findBySection(UUID sectionId);
}
