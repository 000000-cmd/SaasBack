package com.saas.business.domain.port.out;

import com.saas.business.domain.model.BranchOffering;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IBranchOfferingRepositoryPort extends IGenericRepositoryPort<BranchOffering, UUID> {
    List<BranchOffering> findByBranchId(UUID branchId);
}
