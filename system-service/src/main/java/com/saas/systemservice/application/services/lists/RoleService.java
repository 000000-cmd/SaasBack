package com.saas.systemservice.application.services.lists;

import com.saas.systemservice.application.services.GenericCrudService;
import com.saas.systemservice.domain.model.lists.Role;
import com.saas.systemservice.domain.ports.in.lists.IRoleUseCase;
import com.saas.systemservice.domain.ports.out.lists.IRoleRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class RoleService extends GenericCrudService<Role, String> implements IRoleUseCase {


    public RoleService(IRoleRepositoryPort repository) {
        super(repository);
    }

}
