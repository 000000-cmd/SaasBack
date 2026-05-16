package com.saas.system.application.service.business;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.business.BranchType;
import com.saas.system.domain.port.out.business.IBranchTypeRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BranchTypeService extends BaseCatalogService<BranchType, UUID> {

    public BranchTypeService(IBranchTypeRepositoryPort repository){
        super(repository);
    }

    @Override
    public String getCatalogPath() {
        return "branch_type";
    }

    @Override
    public BranchType newInstance() {
        return new BranchType();
    }

    @Override
    protected String getResourceName() {
        return "Tipos de sucursal";
    }
}
