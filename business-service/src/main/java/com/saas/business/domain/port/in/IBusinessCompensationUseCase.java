package com.saas.business.domain.port.in;

import com.saas.business.domain.model.BusinessCompensation;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IBusinessCompensationUseCase extends IGenericUseCase<BusinessCompensation, UUID> {
    List<BusinessCompensation> findByBusiness(UUID businessId);
    Optional<BusinessCompensation> findCurrentByBusiness(UUID businessId);
    BusinessCompensation supersede(UUID currentId, BusinessCompensation incoming);
}
