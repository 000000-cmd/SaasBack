package com.saas.system.application.service;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.RegistrationStatus;
import com.saas.system.domain.port.out.IRegistrationStatusRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegistrationStatusService extends BaseCatalogService<RegistrationStatus, UUID> {

    public RegistrationStatusService(IRegistrationStatusRepositoryPort repository) {
        super(repository);
    }

    @Override
    protected String getResourceName() {
        return "Estado de registro";
    }

    @Override
    public String getCatalogPath() {
        return "registration_status";
    }

    @Override
    public RegistrationStatus newInstance() {
        return new RegistrationStatus();
    }
}
