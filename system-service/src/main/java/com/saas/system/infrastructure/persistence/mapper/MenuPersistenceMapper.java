package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.Menu;
import com.saas.system.infrastructure.persistence.entity.MenuEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre Menu (domain) y MenuEntity (JPA).
 */
@Mapper(componentModel = "spring")
public interface MenuPersistenceMapper extends IBaseMapper<Menu, MenuEntity> {

    @Override
    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    @Mapping(target = "parentId", expression = "java(entity.getParentId() != null ? entity.getParentId().toString() : null)")
    Menu toDomain(MenuEntity entity);

    @Override
    @Mapping(target = "id", expression = "java(domain.getId() != null ? java.util.UUID.fromString(domain.getId()) : null)")
    @Mapping(target = "parentId", expression = "java(domain.getParentId() != null && !domain.getParentId().isBlank() ? java.util.UUID.fromString(domain.getParentId()) : null)")
    MenuEntity toEntity(Menu domain);
}