package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.TourStep;
import com.saas.system.infrastructure.persistence.entity.MenuEntity;
import com.saas.system.infrastructure.persistence.entity.TourStepEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(config = BaseMapStructConfig.class)
public interface TourStepPersistenceMapper extends IBaseMapper<TourStep, TourStepEntity> {

    @Override
    @Mapping(target = "menuId", source = "menu.id")
    @Mapping(target = "menuCode", source = "menu.code")
    TourStep toDomain(TourStepEntity entity);

    @Override
    @Mapping(target = "menu", source = "menuId", qualifiedByName = "menuRef")
    TourStepEntity toEntity(TourStep domain);

    @Named("menuRef")
    default MenuEntity menuRef(UUID id) {
        if (id == null) return null;
        MenuEntity m = new MenuEntity();
        m.setId(id);
        return m;
    }
}
