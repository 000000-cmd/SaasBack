package com.saas.business.domain.port.in;

import com.saas.business.domain.model.BranchCompensation;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IBranchCompensationUseCase extends IGenericUseCase<BranchCompensation, UUID> {
    List<BranchCompensation> findByBranch(UUID branchId);
    Optional<BranchCompensation> findCurrentByBranch(UUID branchId);
    BranchCompensation supersede(UUID currentId, BranchCompensation incoming);
}
