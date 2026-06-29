package com.saas.business.domain.port.out;

import com.saas.business.domain.model.BranchSchedule;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IBranchScheduleRepositoryPort extends IGenericRepositoryPort<BranchSchedule, UUID> {
    List<BranchSchedule> findByBranchId(UUID branchId);
    List<BranchSchedule> findByBranchIdAndValidToIsNull(UUID branchId);
}
