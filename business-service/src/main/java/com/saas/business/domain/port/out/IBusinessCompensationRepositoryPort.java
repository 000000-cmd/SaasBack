package com.saas.business.domain.port.out;

import com.saas.business.domain.model.BusinessCompensation;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IBusinessCompensationRepositoryPort extends IGenericRepositoryPort<BusinessCompensation, UUID> {
    List<BusinessCompensation> findByBusinessId(UUID businessId);
    List<BusinessCompensation> findByBusinessIdAndValidToIsNull(UUID businessId);
}
