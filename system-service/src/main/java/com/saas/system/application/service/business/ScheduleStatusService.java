package com.saas.system.application.service.business;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.business.ScheduleStatus;
import com.saas.system.domain.port.out.business.IScheduleStatusRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ScheduleStatusService extends BaseCatalogService<ScheduleStatus, UUID> {

    public ScheduleStatusService(IScheduleStatusRepositoryPort repository) {
        super(repository);
    }

    @Override
    public String getCatalogPath() {
        return "schedule_status";
    }

    @Override
    public ScheduleStatus newInstance() {
        return new ScheduleStatus();
    }

    @Override
    protected String getResourceName() {
        return "Estado de agenda";
    }
}
