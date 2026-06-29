package com.saas.business.domain.port.in;

import com.saas.business.domain.model.BranchSchedule;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.UUID;

public interface IBranchScheduleUseCase extends IGenericUseCase<BranchSchedule, UUID> {
    List<BranchSchedule> findByBranch(UUID branchId);
    List<BranchSchedule> findCurrentByBranch(UUID branchId);
    BranchSchedule supersede(UUID currentId, BranchSchedule incoming);
}
