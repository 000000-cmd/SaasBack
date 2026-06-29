package com.saas.business.domain.port.out;

import com.saas.business.domain.model.OfferingCategory;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IOfferingCategoryRepositoryPort extends IGenericRepositoryPort<OfferingCategory, UUID> {
    List<OfferingCategory> findByBusinessId(UUID businessId);
}
