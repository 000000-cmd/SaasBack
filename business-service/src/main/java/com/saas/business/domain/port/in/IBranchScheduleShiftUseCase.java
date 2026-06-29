package com.saas.business.domain.port.in;

import com.saas.business.domain.model.BranchScheduleShift;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.UUID;

public interface IBranchScheduleShiftUseCase extends IGenericUseCase<BranchScheduleShift, UUID> {
    List<BranchScheduleShift> findByBranchSchedule(UUID branchScheduleId);
}
