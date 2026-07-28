package com.saas.system.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.system.domain.model.TourStep;

import java.util.List;
import java.util.UUID;

public interface ITourStepRepositoryPort extends IGenericRepositoryPort<TourStep, UUID> {

    /** Todos los pasos ordenados por DisplayOrder, con su menu resuelto. */
    List<TourStep> findAllOrdered();
}
