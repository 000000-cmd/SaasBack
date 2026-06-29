package com.saas.business.domain.port.out;

import com.saas.business.domain.model.Branch;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IBranchRepositoryPort extends IGenericRepositoryPort<Branch, UUID> {
    List<Branch> findByBusinessId(UUID businessId);
}
