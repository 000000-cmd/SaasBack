package com.saas.business.domain.port.out;

import com.saas.business.domain.model.BusinessSchedule;
import com.saas.common.port.out.IGenericRepositoryPort;
import java.util.List;
import java.util.UUID;

public interface IBusinessScheduleRepositoryPort extends IGenericRepositoryPort<BusinessSchedule, UUID> {
    List<BusinessSchedule> findByBusinessId(UUID businessId);
    List<BusinessSchedule> findByBusinessIdAndValidToIsNull(UUID businessId);
}
