package com.saas.business.domain.port.in;

import com.saas.business.domain.model.Branch;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.UUID;

public interface IBranchUseCase extends IGenericUseCase<Branch, UUID> {
    List<Branch> findByBusiness(UUID businessId);
}
