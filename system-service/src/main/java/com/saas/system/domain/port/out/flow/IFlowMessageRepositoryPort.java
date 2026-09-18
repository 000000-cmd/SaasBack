package com.saas.system.domain.port.out.flow;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.flow.FlowMessage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IFlowMessageRepositoryPort extends IGenericRepositoryPort<FlowMessage, UUID> {
    Optional<FlowMessage> findByCode(String code);
    boolean existsByCode(String code);
    List<FlowMessage> findAllOrdered();
}
