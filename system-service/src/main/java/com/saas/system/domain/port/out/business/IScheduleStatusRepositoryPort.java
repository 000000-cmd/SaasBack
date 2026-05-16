package com.saas.system.domain.port.out.business;

import com.saas.common.port.out.ICatalogRepositoryPort;
import com.saas.system.domain.model.business.ScheduleStatus;

import java.util.UUID;

public interface IScheduleStatusRepositoryPort extends ICatalogRepositoryPort<ScheduleStatus, UUID> {
}
