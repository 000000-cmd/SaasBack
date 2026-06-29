package com.saas.system.application.service.business;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.business.ScheduleType;
import com.saas.system.domain.port.out.business.IScheduleTypeRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ScheduleTypeService extends BaseCatalogService<ScheduleType, UUID> {

    public ScheduleTypeService(IScheduleTypeRepositoryPort repository) {
        super(repository);
    }

    @Override
    public String getCatalogPath() {
        return "schedule_type";
    }

    @Override
    public ScheduleType newInstance() {
        return new ScheduleType();
    }

    @Override
    protected String getResourceName() {
        return "Tipo de horario";
    }
}
