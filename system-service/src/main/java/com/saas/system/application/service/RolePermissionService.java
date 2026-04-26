package com.saas.system.application.service;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.Permission;
import com.saas.system.domain.model.RolePermission;
import com.saas.system.domain.port.in.IRolePermissionUseCase;
import com.saas.system.domain.port.out.IPermissionRepositoryPort;
import com.saas.system.domain.port.out.IRolePermissionRepositoryPort;
import com.saas.system.domain.port.out.IRoleRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionService implements IRolePermissionUseCase {

    private final IRolePermissionRepositoryPort rolePermRepo;
    private final IRoleRepositoryPort roleRepo;
    private final IPermissionRepositoryPort permRepo;

    @Override
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByRoleId(UUID roleId) {
        if (!roleRepo.existsById(roleId)) {
            throw new ResourceNotFoundException("Rol", "Id", roleId);
        }
        Set<UUID> permIds = rolePermRepo.findByRoleId(roleId).stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toSet());
        return permIds.stream()
                .map(id -> permRepo.findById(id).orElse(null))
                .filter(p -> p != null)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getPermissionCodesByRoleId(UUID roleId) {
        return rolePermRepo.findPermissionCodesByRoleId(roleId);
    }

    @Override
    @Transactional
    public void replacePermissionsForRole(UUID roleId, Set<UUID> permissionIds) {
        if (!roleRepo.existsById(roleId)) {
            throw new ResourceNotFoundException("Rol", "Id", roleId);
        }
        // Validar que todos los permisos existan
        if (permissionIds != null) {
            for (UUID pid : permissionIds) {
                if (!permRepo.existsById(pid)) {
                    throw new ResourceNotFoundException("Permiso", "Id", pid);
                }
            }
        }
        rolePermRepo.replacePermissionsForRole(roleId, permissionIds);
    }
}
