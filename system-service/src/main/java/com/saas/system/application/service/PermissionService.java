package com.saas.system.application.service;

import com.saas.common.service.GenericCrudService;
import com.saas.system.domain.model.Permission;
import com.saas.system.domain.port.in.IPermissionUseCase;
import com.saas.system.domain.port.out.IPermissionRepositoryPort;
import org.springframework.stereotype.Service;

/**
 * Servicio de aplicación para gestión de Permisos.
 */
@Service
public class PermissionService extends GenericCrudService<Permission, String> implements IPermissionUseCase {

    public PermissionService(IPermissionRepositoryPort repository) {
        super(repository);
    }

    @Override
    protected String getResourceName() {
        return "Permiso";
    }
}