package com.saas.system.application.service;

import com.saas.common.service.CodeCrudService;
import com.saas.system.domain.model.Role;
import com.saas.system.domain.port.in.IRoleUseCase;
import com.saas.system.domain.port.out.IRoleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RoleService extends CodeCrudService<Role, UUID> implements IRoleUseCase {

    private final IRoleRepositoryPort roleRepo;

    public RoleService(IRoleRepositoryPort repo) {
        super(repo);
        this.roleRepo = repo;
    }

    @Override protected String getResourceName() { return "Rol"; }

    @Override
    protected void applyChanges(Role existing, Role incoming) {
        if (incoming.getCode() != null)        existing.setCode(incoming.getCode());
        if (incoming.getName() != null)        existing.setName(incoming.getName());
        if (incoming.getDescription() != null) existing.setDescription(incoming.getDescription());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findByIds(Set<UUID> ids) {
        return ids == null || ids.isEmpty() ? List.of() : roleRepo.findAllByIds(ids);
    }
}
