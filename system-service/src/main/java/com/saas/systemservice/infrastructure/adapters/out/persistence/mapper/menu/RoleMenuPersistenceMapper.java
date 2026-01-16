package com.saas.systemservice.infrastructure.adapters.out.persistence.mapper.menu;

import com.saas.saascommon.infrastructure.mapper.IBaseMapper;
import com.saas.systemservice.domain.model.menu.RoleMenu;
import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.lists.RoleEntity;
import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.menu.MenuEntity;
import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.menu.RoleMenuEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RoleMenuPersistenceMapper extends IBaseMapper<RoleMenu, RoleMenuEntity> {

    @Override
    @Mapping(target = "role", source = "roleId", qualifiedByName = "mapRoleFromId")
    @Mapping(target = "menu", source = "menuId", qualifiedByName = "mapMenuFromId")
    RoleMenuEntity toEntity(RoleMenu domain);

    @Override
    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "roleCode", source = "role.code")
    @Mapping(target = "menuId", source = "menu.id")
    @Mapping(target = "menuCode", source = "menu.code")
    RoleMenu toDomain(RoleMenuEntity entity);

    @Named("mapRoleFromId")
    default RoleEntity mapRoleFromId(String roleId) {
        if (roleId == null) return null;
        RoleEntity role = new RoleEntity();
        role.setId(UUID.fromString(roleId));
        return role;
    }

    @Named("mapMenuFromId")
    default MenuEntity mapMenuFromId(String menuId) {
        if (menuId == null) return null;
        MenuEntity menu = new MenuEntity();
        menu.setId(UUID.fromString(menuId));
        return menu;
    }
}
