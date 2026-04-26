package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.SystemListItem;
import com.saas.system.infrastructure.persistence.entity.SystemListEntity;
import com.saas.system.infrastructure.persistence.entity.SystemListItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(config = BaseMapStructConfig.class)
public interface SystemListItemPersistenceMapper extends IBaseMapper<SystemListItem, SystemListItemEntity> {

    @Override
    @Mapping(target = "listId", source = "list.id")
    SystemListItem toDomain(SystemListItemEntity entity);

    @Override
    @Mapping(target = "list", source = "listId", qualifiedByName = "listRef")
    SystemListItemEntity toEntity(SystemListItem domain);

    @Named("listRef")
    default SystemListEntity listRef(UUID id) {
        if (id == null) return null;
        SystemListEntity l = new SystemListEntity();
        l.setId(id);
        return l;
    }
}
