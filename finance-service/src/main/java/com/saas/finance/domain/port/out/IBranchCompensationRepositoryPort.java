package com.saas.finance.domain.port.out;

import com.saas.finance.domain.model.BranchCompensation;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IBranchCompensationRepositoryPort extends IGenericRepositoryPort<BranchCompensation, UUID> {
    List<BranchCompensation> findByBranchId(UUID branchId);
    List<BranchCompensation> findByBranchIdAndValidToIsNull(UUID branchId);
}
