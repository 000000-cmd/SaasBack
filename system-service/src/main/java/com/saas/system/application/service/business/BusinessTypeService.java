package com.saas.system.application.service.business;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.business.BusinessType;
import com.saas.system.domain.port.out.business.IBusinessTypeRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BusinessTypeService extends BaseCatalogService<BusinessType, UUID> {

    public BusinessTypeService(IBusinessTypeRepositoryPort repository) {
        super(repository);
    }

    @Override
    public String getCatalogPath() {
        return "business_types";
    }

    @Override
    public BusinessType newInstance() {
        return new BusinessType();
    }

    @Override
    protected String getResourceName() {
        return "Tipo de negocio";
    }
}
