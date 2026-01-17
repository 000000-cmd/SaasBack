package com.saas.system.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.Permission;
import com.saas.system.domain.model.RoleMenuPermission;
import com.saas.system.domain.port.in.IRoleMenuPermissionUseCase;
import com.saas.system.domain.port.out.IPermissionRepositoryPort;
import com.saas.system.domain.port.out.IRoleMenuPermissionRepositoryPort;
import com.saas.system.domain.port.out.IRoleMenuRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación para gestión de permisos por RoleMenu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleMenuPermissionService implements IRoleMenuPermissionUseCase {

    private final IRoleMenuPermissionRepositoryPort roleMenuPermissionRepository;
    private final IRoleMenuRepositoryPort roleMenuRepository;
    private final IPermissionRepositoryPort permissionRepository;

    @Override
    @Transactional
    public RoleMenuPermission assignPermission(String roleMenuId, String permissionCode) {
        log.debug("Asignando permiso '{}' al RoleMenu '{}'", permissionCode, roleMenuId);

        // Verificar que el RoleMenu existe
        roleMenuRepository.findById(roleMenuId)
                .orElseThrow(() -> new ResourceNotFoundException("RoleMenu", "ID", roleMenuId));

        // Obtener permiso
        Permission permission = permissionRepository.findByCode(permissionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso", "código", permissionCode));

        // Verificar que no exista la asignación
        if (roleMenuPermissionRepository.existsByRoleMenuIdAndPermissionId(roleMenuId, permission.getId())) {
            throw new BusinessException(
                    String.format("El permiso '%s' ya está asignado al RoleMenu '%s'", permissionCode, roleMenuId));
        }

        // Crear asignación
        RoleMenuPermission entity = RoleMenuPermission.builder()
                .roleMenuId(roleMenuId)
                .permissionId(permission.getId())
                .permissionCode(permissionCode)
                .permissionName(permission.getName())
                .build();
        entity.markAsCreated("SYSTEM");

        RoleMenuPermission saved = roleMenuPermissionRepository.save(entity);
        log.info("Permiso '{}' asignado al RoleMenu '{}' (ID: {})", permissionCode, roleMenuId, saved.getId());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleMenuPermission> getPermissionsByRoleMenuId(String roleMenuId) {
        log.debug("Obteniendo permisos del RoleMenu: {}", roleMenuId);
        return roleMenuPermissionRepository.findByRoleMenuId(roleMenuId);
    }

    @Override
    @Transactional
    public void removePermission(String id) {
        log.debug("Eliminando permiso RoleMenuPermission con ID: {}", id);
        roleMenuPermissionRepository.deleteById(id);
        log.info("Permiso RoleMenuPermission eliminado con ID: {}", id);
    }

    @Override
    @Transactional
    public void removeAllPermissionsFromRoleMenu(String roleMenuId) {
        log.debug("Eliminando todos los permisos del RoleMenu: {}", roleMenuId);
        roleMenuPermissionRepository.deleteByRoleMenuId(roleMenuId);
        log.info("Todos los permisos eliminados del RoleMenu: {}", roleMenuId);
    }
}