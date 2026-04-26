package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.Menu;
import com.saas.system.infrastructure.persistence.entity.MenuEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(config = BaseMapStructConfig.class)
public interface MenuPersistenceMapper extends IBaseMapper<Menu, MenuEntity> {

    @Override
    @Mapping(target = "parentId", source = "parent.id")
    Menu toDomain(MenuEntity entity);

    @Override
    @Mapping(target = "parent", source = "parentId", qualifiedByName = "menuRef")
    MenuEntity toEntity(Menu domain);

    @Named("menuRef")
    default MenuEntity menuRef(UUID id) {
        if (id == null) return null;
        MenuEntity m = new MenuEntity();
        m.setId(id);
        return m;
    }
}
