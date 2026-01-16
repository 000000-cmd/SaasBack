package com.saas.systemservice.application.services.menu;

import com.saas.saascommon.domain.exceptions.BusinessException;
import com.saas.saascommon.domain.exceptions.ResourceNotFoundException;
import com.saas.systemservice.domain.model.lists.Role;
import com.saas.systemservice.domain.model.menu.Menu;
import com.saas.systemservice.domain.model.menu.RoleMenu;
import com.saas.systemservice.domain.ports.in.menu.IRoleMenuUseCase;
import com.saas.systemservice.domain.ports.out.lists.IRoleRepositoryPort;
import com.saas.systemservice.domain.ports.out.menu.IMenuRepositoryPort;
import com.saas.systemservice.domain.ports.out.menu.IRoleMenuRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleMenuService implements IRoleMenuUseCase {

    private final IRoleMenuRepositoryPort roleMenuRepository;
    private final IRoleRepositoryPort roleRepository; // Para buscar el ID del rol
    private final IMenuRepositoryPort menuRepository; // Para buscar el ID del menú

    @Override
    @Transactional
    public RoleMenu create(RoleMenu roleMenu) {
        // 1. Obtener Entidades Padre (Rol y Menú) usando los códigos del Request
        Role role = roleRepository.findByCode(roleMenu.getRoleCode())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleMenu.getRoleCode()));

        Menu menu = menuRepository.findByCode(roleMenu.getMenuCode())
                .orElseThrow(() -> new ResourceNotFoundException("Menú no encontrado: " + roleMenu.getMenuCode()));

        // 2. Validar que no exista ya la asignación (usando IDs)
        if (roleMenuRepository.existsByRoleAndMenu(role.getId(), menu.getId())) {
            throw new BusinessException("El menú " + menu.getCode() + " ya está asignado al rol " + role.getCode());
        }

        // 3. Asignar IDs al modelo de dominio
        roleMenu.setRoleId(role.getId());
        roleMenu.setMenuId(menu.getId());
        roleMenu.setEnabled(true);

        return roleMenuRepository.save(roleMenu);
    }

    @Override
    public List<RoleMenu> getByRoleCode(String roleCode) {
        return roleMenuRepository.findByRoleCode(roleCode);
    }

    @Override
    @Transactional
    public void delete(String id) {
        roleMenuRepository.deleteById(id);
    }
}