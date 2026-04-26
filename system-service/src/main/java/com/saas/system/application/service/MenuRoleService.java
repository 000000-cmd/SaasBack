package com.saas.system.application.service;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.MenuRole;
import com.saas.system.domain.port.in.IMenuRoleUseCase;
import com.saas.system.domain.port.out.IMenuRepositoryPort;
import com.saas.system.domain.port.out.IMenuRoleRepositoryPort;
import com.saas.system.domain.port.out.IRoleRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuRoleService implements IMenuRoleUseCase {

    private final IMenuRoleRepositoryPort menuRoleRepo;
    private final IMenuRepositoryPort menuRepo;
    private final IRoleRepositoryPort roleRepo;

    @Override
    @Transactional
    public void replaceRolesForMenu(UUID menuId, Set<UUID> roleIds) {
        if (!menuRepo.existsById(menuId)) {
            throw new ResourceNotFoundException("Menu", "Id", menuId);
        }
        if (roleIds != null) {
            for (UUID rid : roleIds) {
                if (!roleRepo.existsById(rid)) {
                    throw new ResourceNotFoundException("Rol", "Id", rid);
                }
            }
        }
        menuRoleRepo.replaceRolesForMenu(menuId, roleIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getRoleIdsForMenu(UUID menuId) {
        return menuRoleRepo.findByMenuId(menuId).stream()
                .map(MenuRole::getRoleId)
                .collect(Collectors.toSet());
    }
}
