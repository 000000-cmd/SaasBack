package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.RolePermission;
import com.saas.system.infrastructure.persistence.entity.PermissionEntity;
import com.saas.system.infrastructure.persistence.entity.RoleEntity;
import com.saas.system.infrastructure.persistence.entity.RolePermissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(config = BaseMapStructConfig.class)
public interface RolePermissionPersistenceMapper extends IBaseMapper<RolePermission, RolePermissionEntity> {

    @Override
    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "permissionId", source = "permission.id")
    RolePermission toDomain(RolePermissionEntity entity);

    @Override
    @Mapping(target = "role", source = "roleId", qualifiedByName = "roleRef")
    @Mapping(target = "permission", source = "permissionId", qualifiedByName = "permissionRef")
    RolePermissionEntity toEntity(RolePermission domain);

    @Named("roleRef")
    default RoleEntity roleRef(UUID id) {
        if (id == null) return null;
        RoleEntity r = new RoleEntity();
        r.setId(id);
        return r;
    }

    @Named("permissionRef")
    default PermissionEntity permissionRef(UUID id) {
        if (id == null) return null;
        PermissionEntity p = new PermissionEntity();
        p.setId(id);
        return p;
    }
}
