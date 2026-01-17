package com.saas.system.infrastructure.persistence.mapper;

import com.saas.system.domain.model.RoleMenu;
import com.saas.system.infrastructure.persistence.entity.RoleMenuEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre RoleMenu (domain) y RoleMenuEntity (JPA).
 */
@Mapper(componentModel = "spring")
public interface RoleMenuPersistenceMapper {

    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    @Mapping(target = "roleId", expression = "java(entity.getRole() != null ? entity.getRole().getId().toString() : null)")
    @Mapping(target = "roleCode", expression = "java(entity.getRole() != null ? entity.getRole().getCode() : null)")
    @Mapping(target = "menuId", expression = "java(entity.getMenu() != null ? entity.getMenu().getId().toString() : null)")
    @Mapping(target = "menuCode", expression = "java(entity.getMenu() != null ? entity.getMenu().getCode() : null)")
    @Mapping(target = "menuLabel", expression = "java(entity.getMenu() != null ? entity.getMenu().getLabel() : null)")
    RoleMenu toDomain(RoleMenuEntity entity);

    // No hay toEntity porque las relaciones se manejan en el adaptador
}