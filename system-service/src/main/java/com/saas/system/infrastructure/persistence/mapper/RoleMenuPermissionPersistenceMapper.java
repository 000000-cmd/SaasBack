package com.saas.system.infrastructure.persistence.mapper;

import com.saas.system.domain.model.RoleMenuPermission;
import com.saas.system.infrastructure.persistence.entity.RoleMenuPermissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre RoleMenuPermission (domain) y RoleMenuPermissionEntity (JPA).
 */
@Mapper(componentModel = "spring")
public interface RoleMenuPermissionPersistenceMapper {

    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    @Mapping(target = "roleMenuId", expression = "java(entity.getRoleMenu() != null ? entity.getRoleMenu().getId().toString() : null)")
    @Mapping(target = "permissionId", expression = "java(entity.getPermission() != null ? entity.getPermission().getId().toString() : null)")
    @Mapping(target = "permissionCode", expression = "java(entity.getPermission() != null ? entity.getPermission().getCode() : null)")
    @Mapping(target = "permissionName", expression = "java(entity.getPermission() != null ? entity.getPermission().getName() : null)")
    RoleMenuPermission toDomain(RoleMenuPermissionEntity entity);

    // No hay toEntity porque las relaciones se manejan en el adaptador
}
