package com.saas.system.domain.port.out.business;

import com.saas.common.port.out.ICatalogRepositoryPort;
import com.saas.system.domain.model.business.ScheduleType;

import java.util.UUID;

public interface IScheduleTypeRepositoryPort extends ICatalogRepositoryPort<ScheduleType, UUID> {
}
