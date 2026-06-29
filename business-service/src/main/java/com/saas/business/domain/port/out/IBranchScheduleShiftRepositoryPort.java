package com.saas.business.domain.port.out;

import com.saas.business.domain.model.BranchScheduleShift;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IBranchScheduleShiftRepositoryPort extends IGenericRepositoryPort<BranchScheduleShift, UUID> {
    List<BranchScheduleShift> findByBranchScheduleId(UUID branchScheduleId);
}
