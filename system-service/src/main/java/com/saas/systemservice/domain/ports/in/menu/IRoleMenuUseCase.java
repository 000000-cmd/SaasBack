package com.saas.systemservice.domain.ports.in.menu;

import com.saas.systemservice.domain.model.menu.RoleMenu;

import java.util.List;

public interface IRoleMenuUseCase {

    RoleMenu create(RoleMenu roleMenu);

    List<RoleMenu> getByRoleCode(String roleCode);

    void delete(String id);
}