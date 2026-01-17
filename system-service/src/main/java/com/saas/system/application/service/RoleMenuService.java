package com.saas.system.application.service;

import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import com.saas.system.domain.model.Menu;
import com.saas.system.domain.model.Role;
import com.saas.system.domain.model.RoleMenu;
import com.saas.system.domain.port.in.IRoleMenuUseCase;
import com.saas.system.domain.port.out.IMenuRepositoryPort;
import com.saas.system.domain.port.out.IRoleMenuPermissionRepositoryPort;
import com.saas.system.domain.port.out.IRoleMenuRepositoryPort;
import com.saas.system.domain.port.out.IRoleRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación para gestión de asignaciones de Menús a Roles.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleMenuService implements IRoleMenuUseCase {

    private final IRoleMenuRepositoryPort roleMenuRepository;
    private final IRoleRepositoryPort roleRepository;
    private final IMenuRepositoryPort menuRepository;
    private final IRoleMenuPermissionRepositoryPort roleMenuPermissionRepository;

    @Override
    @Transactional
    public RoleMenu assignMenuToRole(String roleCode, String menuCode) {
        log.debug("Asignando menú '{}' al rol '{}'", menuCode, roleCode);

        // Obtener rol
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", "código", roleCode));

        // Obtener menú
        Menu menu = menuRepository.findByCode(menuCode)
                .orElseThrow(() -> new ResourceNotFoundException("Menú", "código", menuCode));

        // Verificar que no exista la asignación
        if (roleMenuRepository.existsByRoleIdAndMenuId(role.getId(), menu.getId())) {
            throw new BusinessException(
                    String.format("El menú '%s' ya está asignado al rol '%s'", menuCode, roleCode));
        }

        // Crear asignación
        RoleMenu roleMenu = RoleMenu.builder()
                .roleId(role.getId())
                .menuId(menu.getId())
                .roleCode(roleCode)
                .menuCode(menuCode)
                .menuLabel(menu.getLabel())
                .build();
        roleMenu.markAsCreated("SYSTEM");

        RoleMenu saved = roleMenuRepository.save(roleMenu);
        log.info("Menú '{}' asignado al rol '{}' (ID: {})", menuCode, roleCode, saved.getId());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleMenu> getMenusByRoleCode(String roleCode) {
        log.debug("Obteniendo menús del rol: {}", roleCode);
        return roleMenuRepository.findByRoleCode(roleCode);
    }

    @Override
    @Transactional
    public void removeAssignment(String id) {
        log.debug("Eliminando asignación de RoleMenu con ID: {}", id);

        // Primero eliminar los permisos asociados
        roleMenuPermissionRepository.deleteByRoleMenuId(id);

        // Luego eliminar la asignación
        roleMenuRepository.deleteById(id);
        log.info("Asignación RoleMenu eliminada con ID: {}", id);
    }

    @Override
    @Transactional
    public void removeAssignment(String roleCode, String menuCode) {
        log.debug("Eliminando asignación del menú '{}' del rol '{}'", menuCode, roleCode);

        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", "código", roleCode));

        Menu menu = menuRepository.findByCode(menuCode)
                .orElseThrow(() -> new ResourceNotFoundException("Menú", "código", menuCode));

        roleMenuRepository.deleteByRoleIdAndMenuId(role.getId(), menu.getId());
        log.info("Asignación del menú '{}' eliminada del rol '{}'", menuCode, roleCode);
    }
}