package com.saas.system.application.service.business;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.business.Status;
import com.saas.system.domain.port.out.business.IStatusRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StatusService extends BaseCatalogService<Status, UUID> {

    public StatusService(IStatusRepositoryPort repository) {
        super(repository);
    }

    @Override
    public String getCatalogPath() {
        return "status";
    }

    @Override
    public Status newInstance() {
        return new Status();
    }

    @Override
    protected String getResourceName() {
        return "Estado";
    }
}
