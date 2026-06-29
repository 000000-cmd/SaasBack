package com.saas.business.domain.port.in;

import com.saas.business.domain.model.OfferingCategory;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.UUID;

public interface IOfferingCategoryUseCase extends IGenericUseCase<OfferingCategory, UUID> {
    List<OfferingCategory> findByBusiness(UUID businessId);
}
