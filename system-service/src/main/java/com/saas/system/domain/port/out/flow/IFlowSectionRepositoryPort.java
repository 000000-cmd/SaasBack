package com.saas.system.domain.port.out.flow;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.flow.FlowSection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IFlowSectionRepositoryPort extends IGenericRepositoryPort<FlowSection, UUID> {
    List<FlowSection> findByFlow(UUID flowId);
    Optional<FlowSection> findByFlowAndCode(UUID flowId, String code);
}
