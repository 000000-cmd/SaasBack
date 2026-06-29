package com.saas.business.domain.port.in;

import com.saas.business.domain.model.BranchOffering;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.UUID;

public interface IBranchOfferingUseCase extends IGenericUseCase<BranchOffering, UUID> {
    List<BranchOffering> findByBranch(UUID branchId);
}
