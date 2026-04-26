package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.MenuRole;
import com.saas.system.infrastructure.persistence.entity.MenuEntity;
import com.saas.system.infrastructure.persistence.entity.MenuRoleEntity;
import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(config = BaseMapStructConfig.class)
public interface MenuRolePersistenceMapper extends IBaseMapper<MenuRole, MenuRoleEntity> {

    @Override
    @Mapping(target = "menuId", source = "menu.id")
    @Mapping(target = "roleId", source = "role.id")
    MenuRole toDomain(MenuRoleEntity entity);

    @Override
    @Mapping(target = "menu", source = "menuId", qualifiedByName = "menuRef")
    @Mapping(target = "role", source = "roleId", qualifiedByName = "roleRef")
    MenuRoleEntity toEntity(MenuRole domain);

    @Named("menuRef")
    default MenuEntity menuRef(UUID id) {
        if (id == null) return null;
        MenuEntity m = new MenuEntity();
        m.setId(id);
        return m;
    }

    @Named("roleRef")
    default RoleEntity roleRef(UUID id) {
        if (id == null) return null;
        RoleEntity r = new RoleEntity();
        r.setId(id);
        return r;
    }
}
