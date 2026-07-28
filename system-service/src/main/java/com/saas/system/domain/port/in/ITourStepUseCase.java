package com.saas.system.domain.port.in;

import com.saas.common.port.in.IGenericUseCase;
import com.saas.system.domain.model.TourStep;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ITourStepUseCase extends IGenericUseCase<TourStep, UUID> {

    List<TourStep> getAllOrdered();

    /** Pasos visibles para esos roles: los sin menu, mas los de menus permitidos. */
    List<TourStep> getStepsForRoles(Set<UUID> roleIds);
}
