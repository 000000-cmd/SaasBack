package com.saas.systemservice.infrastructure.adapters.out.persistence.mapper.menu;


import com.saas.saascommon.infrastructure.mapper.IBaseMapper;
import com.saas.systemservice.domain.model.menu.Menu;
import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.menu.MenuEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MenuPersistenceMapper extends IBaseMapper<Menu, MenuEntity> {
}
