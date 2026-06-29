package com.saas.business.domain.port.out;

import com.saas.business.domain.model.BusinessScheduleShift;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IBusinessScheduleShiftRepositoryPort extends IGenericRepositoryPort<BusinessScheduleShift, UUID> {
    List<BusinessScheduleShift> findByBusinessScheduleId(UUID businessScheduleId);
}
