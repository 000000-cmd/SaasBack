package com.saas.system.application.service;

import com.saas.common.service.CodeCrudService;
import com.saas.system.domain.model.SystemList;
import com.saas.system.domain.port.in.ISystemListUseCase;
import com.saas.system.domain.port.out.ISystemListRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SystemListService extends CodeCrudService<SystemList, UUID> implements ISystemListUseCase {

    public SystemListService(ISystemListRepositoryPort repo) { super(repo); }

    @Override protected String getResourceName() { return "Lista del sistema"; }

    @Override
    protected void applyChanges(SystemList existing, SystemList incoming) {
        if (incoming.getCode() != null)        existing.setCode(incoming.getCode());
        if (incoming.getName() != null)        existing.setName(incoming.getName());
        if (incoming.getDescription() != null) existing.setDescription(incoming.getDescription());
    }
}
