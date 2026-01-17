package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.Permission;
import com.saas.system.infrastructure.persistence.entity.PermissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre Permission (domain) y PermissionEntity (JPA).
 */
@Mapper(componentModel = "spring")
public interface PermissionPersistenceMapper extends IBaseMapper<Permission, PermissionEntity> {

    @Override
    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    Permission toDomain(PermissionEntity entity);

    @Override
    @Mapping(target = "id", expression = "java(domain.getId() != null ? java.util.UUID.fromString(domain.getId()) : null)")
    PermissionEntity toEntity(Permission domain);
}