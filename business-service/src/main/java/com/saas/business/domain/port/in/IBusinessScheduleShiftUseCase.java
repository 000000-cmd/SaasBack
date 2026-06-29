package com.saas.business.domain.port.in;

import com.saas.business.domain.model.BusinessScheduleShift;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.UUID;

public interface IBusinessScheduleShiftUseCase extends IGenericUseCase<BusinessScheduleShift, UUID> {
    List<BusinessScheduleShift> findByBusinessSchedule(UUID businessScheduleId);
}
