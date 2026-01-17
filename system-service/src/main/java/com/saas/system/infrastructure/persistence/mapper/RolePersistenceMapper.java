package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.Role;
import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre Role (domain) y RoleEntity (JPA).
 */
@Mapper(componentModel = "spring")
public interface RolePersistenceMapper extends IBaseMapper<Role, RoleEntity> {

    @Override
    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    Role toDomain(RoleEntity entity);

    @Override
    @Mapping(target = "id", expression = "java(domain.getId() != null ? java.util.UUID.fromString(domain.getId()) : null)")
    RoleEntity toEntity(Role domain);
}