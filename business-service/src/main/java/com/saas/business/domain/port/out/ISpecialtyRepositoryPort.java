package com.saas.business.domain.port.out;

import com.saas.business.domain.model.Specialty;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface ISpecialtyRepositoryPort extends IGenericRepositoryPort<Specialty, UUID> {
    List<Specialty> findByBusinessId(UUID businessId);
}
