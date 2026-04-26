package com.saas.system.application.service;

import com.saas.common.service.CodeCrudService;
import com.saas.system.domain.model.Permission;
import com.saas.system.domain.port.in.IPermissionUseCase;
import com.saas.system.domain.port.out.IPermissionRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PermissionService extends CodeCrudService<Permission, UUID> implements IPermissionUseCase {

    public PermissionService(IPermissionRepositoryPort repo) { super(repo); }

    @Override protected String getResourceName() { return "Permiso"; }

    @Override
    protected void applyChanges(Permission existing, Permission incoming) {
        if (incoming.getCode() != null)        existing.setCode(incoming.getCode());
        if (incoming.getName() != null)        existing.setName(incoming.getName());
        if (incoming.getDescription() != null) existing.setDescription(incoming.getDescription());
    }
}
