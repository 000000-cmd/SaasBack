package com.saas.system.application.service;

import com.saas.common.service.GenericCrudService;
import com.saas.system.domain.model.Role;
import com.saas.system.domain.port.in.IRoleUseCase;
import com.saas.system.domain.port.out.IRoleRepositoryPort;
import org.springframework.stereotype.Service;

/**
 * Servicio de aplicación para gestión de Roles.
 */
@Service
public class RoleService extends GenericCrudService<Role, String> implements IRoleUseCase {

    public RoleService(IRoleRepositoryPort repository) {
        super(repository);
    }

    @Override
    protected String getResourceName() {
        return "Rol";
    }
}
