package com.saas.system.application.service;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.Gender;
import com.saas.system.domain.port.out.IGenderRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GenderService extends BaseCatalogService<Gender, UUID> {

    public GenderService(IGenderRepositoryPort repository) {
        super(repository);
    }

    @Override
    protected String getResourceName() {
        return "Genero";
    }

    @Override
    public String getCatalogPath() {
        return "genders";
    }

    @Override
    public Gender newInstance() {
        return new Gender();
    }
}
