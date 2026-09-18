package com.saas.system.domain.port.out.flow;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.flow.Flow;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IFlowRepositoryPort extends IGenericRepositoryPort<Flow, UUID> {
    Optional<Flow> findByCode(String code);
    boolean existsByCode(String code);
    List<Flow> findAllOrdered();
}
