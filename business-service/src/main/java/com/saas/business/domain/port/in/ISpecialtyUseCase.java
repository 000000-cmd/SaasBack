package com.saas.business.domain.port.in;

import com.saas.business.domain.model.Specialty;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.UUID;

public interface ISpecialtyUseCase extends IGenericUseCase<Specialty, UUID> {
    List<Specialty> findByBusiness(UUID businessId);
}
